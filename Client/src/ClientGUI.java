import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class ClientGUI {
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton, clientsButton, bannedButton;
    private JComboBox<String> recipientBox; // Dropdown for recipients

    private PrintWriter out;
    private DefaultComboBoxModel<String> recipientModel;

    public ClientGUI(Socket socket) {
        try {
            // Initialize IO streams
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Initialize the frame
            frame = new JFrame("Chat Client");
            frame.setSize(600, 400);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Chat area
            chatArea = new JTextArea();
            chatArea.setEditable(false);
            JScrollPane chatScrollPane = new JScrollPane(chatArea);

            // Message input
            messageField = new JTextField();
            sendButton = new JButton("Send");
            sendButton.addActionListener(e -> sendMessage());

            // Dropdown for recipient selection
            recipientModel = new DefaultComboBoxModel<>();
            recipientBox = new JComboBox<>(recipientModel);
            recipientBox.addItem("All"); // Default option to send to all clients

            // Command buttons
            clientsButton = new JButton("List Clients");
            bannedButton = new JButton("Banned Phrases");

            clientsButton.addActionListener(e -> sendCommand("/clients"));
            bannedButton.addActionListener(e -> sendCommand("/banned"));

            // Layout setup
            JPanel inputPanel = new JPanel(new BorderLayout());
            inputPanel.add(recipientBox, BorderLayout.WEST); // Add recipient dropdown
            inputPanel.add(messageField, BorderLayout.CENTER);
            inputPanel.add(sendButton, BorderLayout.EAST);

            JPanel buttonPanel = new JPanel(new FlowLayout());
            buttonPanel.add(clientsButton);
            buttonPanel.add(bannedButton);

            frame.getContentPane().add(chatScrollPane, BorderLayout.CENTER);
            frame.getContentPane().add(inputPanel, BorderLayout.SOUTH);
            frame.getContentPane().add(buttonPanel, BorderLayout.NORTH);

            // Show the GUI
            frame.setVisible(true);

            // Start a thread to read messages from the server
            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        appendMessage(serverMessage);

                        // Update the dropdown list when a client list update is received
                        if (serverMessage.startsWith("Connected clients: ")) {
                            updateRecipientList(serverMessage);
                        }
                    }
                } catch (IOException e) {
                    appendMessage("Disconnected from server.");
                }
            }).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error connecting to the server: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendMessage() {
        String message = messageField.getText();
        if (!message.isEmpty()) {
            String selectedRecipient = (String) recipientBox.getSelectedItem();
            if (selectedRecipient.equals("All")) {
                out.println(message); // Send to all
            } else {
                out.println("/msg " + selectedRecipient + " " + message); // Send private message
            }
            messageField.setText("");
        }
    }

    private void sendCommand(String command) {
        out.println(command);
    }

    private void appendMessage(String message) {
        chatArea.append(message + "\n");

        if (message.startsWith("Connected clients: ")) {
            updateRecipientList(message); // Update dropdown with clients
        } else if (message.startsWith("Banned phrases: ")) {
            JOptionPane.showMessageDialog(frame, message, "Banned Phrases", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // Parse and update the recipient dropdown
    private void updateRecipientList(String clientListMessage) {
        SwingUtilities.invokeLater(() -> {
            recipientModel.removeAllElements(); // Clear the existing list
            recipientModel.addElement("All");  // Default option to broadcast

            String clientList = clientListMessage.replace("Connected clients: ", "").trim();
            if (!clientList.equals("None")) {
                String[] clients = clientList.split(", ");
                for (String client : clients) {
                    recipientModel.addElement(client); // Add each client to the dropdown
                }
            }
        });
    }

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 1234);
            new ClientGUI(socket);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Unable to connect to the server.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}