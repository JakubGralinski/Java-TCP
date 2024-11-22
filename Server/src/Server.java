import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Server {
    private int port;

    String serverName;

    private List<String> bannedPhrases;
    private List<ClientHandler> connectedClients = new ArrayList<>();

    private ServerGUI gui;

    public static final String configFilePath = "src/config.json";

    public void setGui(ServerGUI gui) {
        this.gui = gui;
    }

    public Server() throws IOException {
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

            System.out.println("Server configuration loaded:");
            System.out.println("Name: " + serverName);
            System.out.println("Port: " + port);
            System.out.println("Banned Phrases: " + bannedPhrases);
        }
    }

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println(serverName + " running on port: " + port);

            try (FileReader configLoader = new FileReader(configFilePath)) {
                JSONTokener tokener = new JSONTokener(configLoader);
                JSONObject config = new JSONObject(tokener);
                int numberOfClients = config.getInt("numberOfClients");

                for (int i = 0; i < numberOfClients; i++) {
                    try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getInetAddress());
                    ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                    new Thread(clientHandler).start();
                    }
                    catch(Exception _){
                        System.out.println("Too many client connections: " + numberOfClients);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public List<String> getBannedPhrases() {
        return bannedPhrases;
    }

    public synchronized void broadcastMessage(String message, ClientHandler sender) {
        String senderNickname = (sender != null) ? sender.nickname : "Server";
        //logMessage(message, senderNickname, true);

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

        System.out.println(logMessage);
        if (gui != null) gui.appendLog(logMessage);
        if (gui != null) gui.updateClientList(connectedClients.stream()
                .map(client -> client.nickname)
                .toList());
        updateAllClients();
    }

    public synchronized void removeClient(ClientHandler clientHandler) {
        connectedClients.remove(clientHandler);
        String logMessage = "Client " + clientHandler.nickname + " disconnected. Total clients: " + connectedClients.size();
        broadcastMessage("User " + clientHandler.nickname + " has disconnected.", null);
        //logMessage("User " + clientHandler.nickname + " has disconnected.", "Server", true);

        System.out.println(logMessage);
        if (gui != null) gui.appendLog(logMessage);
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

    public synchronized void sendPrivateMessage(String senderNickname, String recipientNickname, String message) {
        boolean recipentFound = false;
        String recipentName="";
        for (ClientHandler client : connectedClients) {
            if (client.nickname.equalsIgnoreCase(recipientNickname)) {
                client.sendMessage("[Private] " + senderNickname + " -> " + recipientNickname + ": " +message);
                recipentFound = true;
                recipentName = client.nickname;
            }
        }
        if(recipentFound){
            for (ClientHandler client : connectedClients) {
                if(client.nickname.equalsIgnoreCase(recipientNickname)) break;
                if (client.nickname.equalsIgnoreCase(senderNickname)) {
                    client.sendMessage("[Private] " + senderNickname + " -> " + recipientNickname + ": " +message);
                    break;
                }
            }
        }else{
            for (ClientHandler client : connectedClients) {
                if (client.nickname.equalsIgnoreCase(senderNickname)) {
                    client.sendMessage("User " + recipientNickname + " not found.");
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
        if (clientList.length() > 19) { // Length of "Connected clients: "
            clientList.setLength(clientList.length() - 2); // Remove trailing ", "
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
    public synchronized void sendMessageToMultipleClients(String senderNickname, String[] recipients, String message) {
        for (String recipient : recipients) {
            boolean found = false;
            for (ClientHandler client : connectedClients) {
                if (client.nickname.equalsIgnoreCase(recipient.trim())) {
                    client.sendMessage("(Group) " + senderNickname + ": " + message);
                    found = true;
                }
            }
            if (!found) {
                sendPrivateMessage(senderNickname, recipient.trim(), "User not found in group message.");
            }
        }
    }

    public synchronized void broadcastMessageExcluding(String senderNickname, String[] exclusions, String message) {
        List<String> excludedList = Arrays.asList(exclusions);
        for (ClientHandler client : connectedClients) {
            if (!excludedList.contains(client.nickname)) {
                client.sendMessage("(Excluded) " + senderNickname + ": " + message);
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
}