/*
import java.io.*;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 1234);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println(in.readLine());
            String name;
            while ((name = userInput.readLine()) != null) {
                out.println(name);
                String response = in.readLine();
                if (!response.contains("Nickname already taken")) {
                    break;
                }
                System.out.println(response);
            }

            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    System.err.println("Disconnected from server.");
                }
            }).start();

            String userMessage;
            while ((userMessage = userInput.readLine()) != null) {
                if (userMessage.equalsIgnoreCase("/quit")) {
                    System.out.println("Disconnecting...");
                    break;
                }
                out.println(userMessage);
            }
        } catch (IOException e) {
            System.err.println("Error connecting to the server: " + e.getMessage());
        }
    }
}*/
