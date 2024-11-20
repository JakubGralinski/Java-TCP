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
                if (message.equalsIgnoreCase("/clients")) {
                    server.sendConnectedClients(this);
                } else if (message.startsWith("/msg ")) {
                    String[] parts = message.split(" ", 3);
                    if (parts.length == 3) {
                        String recipientNickname = parts[1];
                        String privateMessage = parts[2];
                        server.sendPrivateMessage(nickname, recipientNickname, privateMessage);
                    } else {
                        out.println("Usage: /msg <recipient> <message>");
                    }
                } else {
                    server.broadcastMessage(nickname + ": " + message, this);
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

    public void sendMessage(String message) {
        out.println(message);
    }
}