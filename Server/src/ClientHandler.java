import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private Server server;
    private PrintWriter out;
    private BufferedReader in;
    String nickname;

    public ClientHandler(Socket clientSocket, Server server) {
        this.clientSocket = clientSocket;
        this.server = server;
        try {
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out.println("Welcome to " + server.serverName + "! Enter your nickname:");
        } catch (IOException e) {
            System.err.println("Error setting up client handler: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            // Handle nickname setup
            while (true) {
                nickname = in.readLine();
                if (nickname == null || nickname.isBlank()) {
                    out.println("Nickname cannot be blank. Please enter a valid nickname:");
                } else if (server.isNicknameTaken(nickname)) {
                    out.println("Nickname already taken. Please choose a different nickname:");
                } else {
                    server.addClient(this); // Register client with the server
                    break;
                }
            }

            // Main message-handling loop
            String message;
            while ((message = in.readLine()) != null) {
                if (isInBannedPhrases(message)) {
                    out.println("Your message contains a banned phrase and was not sent.");
                    continue; // Skip further processing
                }

                if (message.startsWith("/clients")) {
                    server.sendConnectedClients(this); // Send the updated client list
                } else if (message.startsWith("/banned")) {
                    sendBannedPhrases(); // Send the list of banned phrases
                } else if (message.startsWith("/msg ")) {
                    handlePrivateMessage(message);
                } else {
                    server.broadcastMessage(nickname + ": " + message, this);
                }
            }
        } catch (IOException e) {
            System.err.println("Client disconnected: " + nickname);
        } finally {
            server.removeClient(this);
            server.broadcastMessage("User " + nickname + " has disconnected.", null);
            closeConnection();
        }
    }

    private void sendBannedPhrases() {
        String bannedList = "Banned phrases: " + String.join(", ", server.getBannedPhrases());
        sendMessage(bannedList);
    }

    // Helper method to handle private messages
    private void handlePrivateMessage(String message) {
        String[] parts = message.split(" ", 3);
        if (parts.length < 3) {
            out.println("Usage: /msg <recipient> <message>");
            return;
        }

        String recipientNickname = parts[1];
        String privateMessage = parts[2];

        server.sendPrivateMessage(nickname, recipientNickname, privateMessage);
    }

    // Check for banned phrases
    private boolean isInBannedPhrases(String message) {
        for (String phrase : server.getBannedPhrases()) {
            if (message.toLowerCase().contains(phrase.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    // Send a message to this client
    public void sendMessage(String message) {
        out.println(message);
    }

    // Close client socket
    private void closeConnection() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing client socket: " + e.getMessage());
        }
    }
}