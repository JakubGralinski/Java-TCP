import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    Socket clientSocket;
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
                if (server.isNicknameTaken(nickname)) {
                    out.println("Nickname already taken. Please choose a different nickname:");
                } else {
                    server.addClient(this);
                    break;
                }
            }

            String message;
            while ((message = in.readLine()) != null) {
                if (isInBannedPhrases(message)) {
                    out.println("Your message contains a banned phrase and was not sent.");
                    continue; // Skip processing the message further
                }
                switch (message.toLowerCase()) {
                    case "/clients":
                        server.sendConnectedClients(this);
                        break;

                    case "/msg":
                        String[] msgParts = message.split(" ", 2); // Extract recipient and message
                        if (msgParts.length == 2) {
                            String recipientNickname = msgParts[0];
                            String privateMessage = msgParts[1];
                            server.sendPrivateMessage(nickname, recipientNickname, privateMessage);
                        } else {
                            out.println("Usage: /msg <recipient> <message>");
                        }
                        break;

                    case "/multi":
                        String[] multiParts = message.split(" ", 2); // Extract recipients and message
                        if (multiParts.length == 2) {
                            String[] recipients = multiParts[0].split(","); // List of recipients
                            String multiMessage = multiParts[1];
                            server.sendMessageToMultipleClients(nickname, recipients, multiMessage);
                        } else {
                            out.println("Usage: /multi <recipient1>,<recipient2>,... <message>");
                        }
                        break;

                    case "/exclude":
                        String[] excludeParts = message.split(" ", 2); // Extract exclusions and message
                        if (excludeParts.length == 2) {
                            String[] exclusions = excludeParts[0].split(","); // List of exclusions
                            String excludeMessage = excludeParts[1];
                            server.broadcastMessageExcluding(nickname, exclusions, excludeMessage);
                        } else {
                            out.println("Usage: /exclude <nickname1>,<nickname2>,... <message>");
                        }
                        break;

                    case "/banned":
                        String bannedList = String.join(", ", server.getBannedPhrases());
                        out.println("Banned phrases: " + bannedList);
                        break;

                    default:
                        // Broadcast message if no command is matched
                        server.broadcastMessage(nickname + ": " + message, this);
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("Client disconnected.");
        } finally {
            server.removeClient(this);
            server.broadcastMessage("User " + nickname + " has disconnected.", null);

            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
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
}