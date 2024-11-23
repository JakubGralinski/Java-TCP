import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ServerGUI {
    private JFrame frame;
    private JTextArea logArea;
    private DefaultListModel<String> clientListModel;

    public ServerGUI(Server server) {
        // Initialize the frame
        frame = new JFrame(server.serverName + " - Server");
        frame.setSize(500, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Logs panel
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);

        // Connected clients panel
        clientListModel = new DefaultListModel<>();
        JList<String> clientList = new JList<>(clientListModel);
        clientList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        clientList.setLayoutOrientation(JList.VERTICAL);
        JScrollPane clientScrollPane = new JScrollPane(clientList);

        // Layout setup
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, clientScrollPane, logScrollPane);
        splitPane.setDividerLocation(150);

        frame.getContentPane().add(splitPane);

        frame.setVisible(true);

        new Thread(() -> {
            try {
                server.run();
            } catch (Exception e) {
                appendLog("Server error: " + e.getMessage());
            }
        }).start();
    }

    public void appendLog(String message) {
        logArea.append(message + "\n");
    }

    public void updateClientList(java.util.List<String> clients) {
        SwingUtilities.invokeLater(() -> {
            clientListModel.clear();
            for (String client : clients) {
                clientListModel.addElement(client);
            }
        });
    }

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        ServerGUI serverGUI = new ServerGUI(server);

        server.setGui(serverGUI);
    }
}