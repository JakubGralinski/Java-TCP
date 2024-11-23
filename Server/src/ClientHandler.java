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
            while (true) {
                nickname = in.readLine();
                if (nickname == null || nickname.isBlank()) {
                    out.println("Nickname cannot be blank. Please enter a valid nickname:");
                } else if (server.isNicknameTaken(nickname)) {
                    out.println("Nickname already taken. Please choose a different nickname:");
                } else {
                    server.addClient(this);
                    break;
                }
            }

            String message;
            //message handler loop
            while ((message = in.readLine()) != null) {
                if (isInBannedPhrases(message)) {
                    out.println("Your message contains a banned phrase and was not sent.");
                    continue;
                }

                if (message.startsWith("/clients")) {
                    server.sendConnectedClients(this);
                } else if (message.startsWith("/banned")) {
                    sendBannedPhrases();
                } else if (message.startsWith("/")) {
                    handleUserListMessage(message); // Handle messages with specified users
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

    private void handleUserListMessage(String message) {
        // Expected format: /user1,user2,user3 <message>
        String[] parts = message.substring(1).split(" ", 2); // Remove the leading '/' and split into recipients and message
        if (parts.length < 2) {
            out.println("Usage: /user1,user2,... <message>");
            return;
        }

        String recipientsStr = parts[0]; // e.g., "user1,user2,user3"
        String actualMessage = parts[1];

        String[] targetUsers = recipientsStr.split(","); // Split recipients by comma

        // Trim whitespace from usernames
        for (int i = 0; i < targetUsers.length; i++) {
            targetUsers[i] = targetUsers[i].trim();
        }

        // Pass the recipients and the message to the server
        server.sendToMultipleClients(nickname, targetUsers, actualMessage);
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