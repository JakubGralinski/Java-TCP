import javax.sound.sampled.Port;
import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private Server server;
    private PrintWriter out;
    private BufferedReader in;
    private int port;
    String nickname;

    public ClientHandler(Socket clientSocket, Server server) {
        this.clientSocket = clientSocket;
        this.server = server;
        this.port = clientSocket.getPort();
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
                    sendInstructions();
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

                if (message.startsWith("/clients")) {//command from client list button
                    server.sendConnectedClients(this);
                } else if (message.startsWith("/banned")) {//command from banned phrases button
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
    private void sendInstructions() {
        String instructions = "Instructions:\n" +
                "- To send a message to all: Simply type your message and press Send.\n" +
                "- To send a message to specific user: Select the user from the list and type your message.\n" +
                "- To send a message to everyone except certain users: Select users without the one u want to avoid.\n" +
                "- To see the list of banned phrases:  click Banned Phrases button.\n"+
                "- To see the list of connected Clients: click List Clients button";
        sendMessage(instructions);
    }

    private void sendBannedPhrases() {
        String bannedList = "Banned phrases: " + String.join(", ", server.getBannedPhrases());
        sendMessage(bannedList);
    }

    private void handleUserListMessage(String message) {
        // /user1,user2,user3 <message>
        String[] parts = message.substring(1).split(" ", 2); // Remove the leading '/' and split into recipients and message

        String recipientsStr = parts[0];
        String actualMessage = parts[1];

        String[] targetUsers = recipientsStr.split(",");

        // Trim whitespace from usernames
        for (int i = 0; i < targetUsers.length; i++) {
            targetUsers[i] = targetUsers[i].trim();
        }

        // Pass the recipients and the message to the server
        server.sendToMultipleClients(nickname, targetUsers, actualMessage);
    }

    private boolean isInBannedPhrases(String message) {
        for (String phrase : server.getBannedPhrases()) {
            if (message.toLowerCase().contains(phrase.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    private void closeConnection() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing client socket: " + e.getMessage());
        }
    }
}