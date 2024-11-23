import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private int port;
    String serverName;

    private List<String> bannedPhrases;
    private List<ClientHandler> connectedClients = new ArrayList<>();

    private ServerGUI gui;

    //public static final String configFilePath = "src/config.json";

    private List<String> serverLogs = new ArrayList<>();

    public synchronized void setGui(ServerGUI gui) {
        this.gui = gui;
        for (String message : serverLogs) {
            gui.appendLog(message);
        }
        serverLogs.clear();
    }

    public synchronized void log(String message) {
        System.out.println(message);
        if (gui != null) {
            gui.appendLog(message);
        } else {
            serverLogs.add(message);
        }
    }

    public Server() throws IOException {
        String configFilePath = "src/config.json";
        try (FileReader configLoader = new FileReader(configFilePath)) {
            JSONTokener tokener = new JSONTokener(configLoader);
            JSONObject config = new JSONObject(tokener);

            this.port = config.getInt("port");
            this.serverName = config.getString("serverName");
            this.bannedPhrases = new ArrayList<>();
            JSONArray phrasesArray = config.getJSONArray("bannedPhrases");
            for (int i = 0; i < phrasesArray.length(); i++) {
                this.bannedPhrases.add(phrasesArray.getString(i));
            }

            log("Server configuration loaded");
            log("Name: " + serverName);
            log("Port: " + port);
            log("Banned Phrases: " + bannedPhrases);
        }
    }

    public void run() {
        String configFilePath = "src/config.json";
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log(serverName + " running on port: " + port);

            try (FileReader configLoader = new FileReader(configFilePath)) {
                JSONTokener tokener = new JSONTokener(configLoader);
                JSONObject config = new JSONObject(tokener);
                int numberOfClients = config.getInt("numberOfClients");

                for (int i = 0; i < numberOfClients; i++) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        log("New client connected: " + clientSocket.getInetAddress());
                        ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                        new Thread(clientHandler).start();
                    } catch (Exception _) {
                        log("Too many client connections: " + numberOfClients);
                    }
                }
            }
        } catch (IOException e) {
            log("Server error: " + e.getMessage());
        }
    }

    public List<String> getBannedPhrases() {
        return bannedPhrases;
    }

    public synchronized void broadcastMessage(String message, ClientHandler sender) {
        String senderNickname = (sender != null) ? sender.nickname : "Server";

        for (ClientHandler client : connectedClients) {
            if (client != sender) {
                client.sendMessage("[All] " + senderNickname + ": " + message);
            }
        }
    }

    public synchronized void addClient(ClientHandler clientHandler) {
        connectedClients.add(clientHandler);
        String logMessage = "Client " + clientHandler.nickname + " connected. Total clients: " + connectedClients.size();
        broadcastMessage("User " + clientHandler.nickname + " has joined the chat.", null);

        log(logMessage);
        if (gui != null) gui.updateClientList(connectedClients.stream()
                .map(client -> client.nickname)
                .toList());
        updateAllClients();
    }

    public synchronized void removeClient(ClientHandler clientHandler) {
        connectedClients.remove(clientHandler);
        String logMessage = "Client " + clientHandler.nickname + " disconnected. Total clients: " + connectedClients.size();
        broadcastMessage("User " + clientHandler.nickname + " has disconnected.", null);

        log(logMessage);
        if (gui != null) gui.updateClientList(connectedClients.stream()
                .map(client -> client.nickname)
                .toList());
        updateAllClients();
    }

    public synchronized boolean isNicknameTaken(String nickname) {
        for (ClientHandler client : connectedClients) {
            if (client.nickname != null && client.nickname.equalsIgnoreCase(nickname)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void sendToMultipleClients(String senderNickname, String[] targetUsers, String message) {
        boolean atLeastOneFound = false;

        for (String target : targetUsers) {
            boolean found = false;
            for (ClientHandler client : connectedClients) {
                if (client.nickname.equalsIgnoreCase(target.trim())) {
                    client.sendMessage("[Private] " + senderNickname + " -> " + target + ": " + message);
                    found = true;
                    atLeastOneFound = true;
                }
            }

            if (!found) {
                for (ClientHandler client : connectedClients) {
                    if (client.nickname.equalsIgnoreCase(senderNickname)) {
                        client.sendMessage("[Error] User not found: " + target);
                        break;
                    }
                }
            }
        }

        if (atLeastOneFound) {
            //Notify about mess delivery
            for (ClientHandler client : connectedClients) {
                if (client.nickname.equalsIgnoreCase(senderNickname)) {
                    client.sendMessage("[Private] Message sent to: " + String.join(", ", targetUsers));
                    break;
                }
            }
        }
    }

    public synchronized void sendConnectedClients(ClientHandler requester) {
        StringBuilder clientList = new StringBuilder("Connected clients: ");
        for (ClientHandler client : connectedClients) {
            clientList.append(client.nickname).append(", ");
        }
        if (clientList.length() > 19) {
            clientList.setLength(clientList.length() - 2);
        } else {
            clientList.append("None");
        }
        requester.sendMessage(clientList.toString());
    }

    private synchronized void updateAllClients() {
        StringBuilder clientList = new StringBuilder("Connected clients: ");
        for (ClientHandler client : connectedClients) {
            clientList.append(client.nickname).append(", ");
        }
        if (clientList.length() > 19) {
            clientList.setLength(clientList.length() - 2);
        } else {
            clientList.append("None");
        }

        String clientListMessage = clientList.toString();
        for (ClientHandler client : connectedClients) {
            client.sendMessage(clientListMessage);
        }
    }
}

    /*private synchronized void logMessage(String message, String sender, boolean isBroadcast) {
        try {
            File logFile = new File("chat_log.json");
            JSONArray logArray;

            if (logFile.exists() && logFile.length() > 0) {
                try (FileReader fr = new FileReader(logFile)) {
                    logArray = new JSONArray(new JSONTokener(fr));
                }
            } else {
                logArray = new JSONArray();
            }

            JSONObject logEntry = new JSONObject();
            logEntry.put("timestamp", System.currentTimeMillis());
            logEntry.put("sender", sender);
            logEntry.put("message", message);
            logEntry.put("broadcast", isBroadcast);

            logArray.put(logEntry);

            try (FileWriter fw = new FileWriter(logFile)) {
                fw.write(logArray.toString(4));
            }
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }*/
