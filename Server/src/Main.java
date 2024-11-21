import java.io.IOException;

public static void main(String[] args) {
    try {
        Server server = new Server();
        server.run();
    } catch (IOException e) {
        System.err.println("Failed to start server: " + e.getMessage());
    }
}