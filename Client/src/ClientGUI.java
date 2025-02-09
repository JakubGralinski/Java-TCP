import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientGUI {
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton, clientsButton, bannedButton;
    private JList<String> recipientList;
    private DefaultListModel<String> recipientListModel;

    private PrintWriter out;

    public ClientGUI(Socket socket) {
        try {
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

            // Recipient list
            recipientListModel = new DefaultListModel<>();
            recipientList = new JList<>(recipientListModel);
            recipientList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            recipientList.setVisibleRowCount(3);
            JScrollPane recipientScrollPane = new JScrollPane(recipientList);
            recipientScrollPane.setPreferredSize(new Dimension(150, 80));
            recipientListModel.addElement("All");

            // Command buttons
            clientsButton = new JButton("List Clients");
            bannedButton = new JButton("Banned Phrases");

            clientsButton.addActionListener(e -> sendCommand("/clients"));
            bannedButton.addActionListener(e -> sendCommand("/banned"));

            // Layout setup
            JPanel inputPanel = new JPanel(new BorderLayout());
            inputPanel.add(recipientScrollPane, BorderLayout.WEST);
            inputPanel.add(messageField, BorderLayout.CENTER);
            inputPanel.add(sendButton, BorderLayout.EAST);

            JPanel buttonPanel = new JPanel(new FlowLayout());
            buttonPanel.add(clientsButton);
            buttonPanel.add(bannedButton);

            frame.getContentPane().add(chatScrollPane, BorderLayout.CENTER);
            frame.getContentPane().add(inputPanel, BorderLayout.SOUTH);
            frame.getContentPane().add(buttonPanel, BorderLayout.NORTH);

            frame.setVisible(true);

            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        appendMessage(serverMessage);

                        // recipent list gets client list
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
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            List<String> selectedRecipients = recipientList.getSelectedValuesList();
            if (selectedRecipients.isEmpty() || selectedRecipients.contains("All")) {
                //if "All" is selected or no recipient is selected
                out.println(message);
                appendMessage("You (to All): " + message);
            } else {
                String recipientsStr = String.join(",", selectedRecipients);

                out.println("/" + recipientsStr + " " + message);
                appendMessage("You (to " + recipientsStr + "): " + message);
            }
            messageField.setText(""); // Clear the input field
        }
    }

    private void sendCommand(String command) {
        out.println(command);
    }

    private void appendMessage(String message) {
        chatArea.append(message + "\n");

        if (message.startsWith("Connected clients: ")) {
            updateRecipientList(message);
        } else if (message.startsWith("Banned phrases: ")) {
            JOptionPane.showMessageDialog(frame, message, "Banned Phrases", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void updateRecipientList(String clientListMessage) {
        SwingUtilities.invokeLater(() -> {
            String previouslySelected = recipientList.getSelectedValue();

            recipientListModel.clear(); // Clear the existing list
            recipientListModel.addElement("All"); // Ensure "All" remains in the list

            String clientList = clientListMessage.replace("Connected clients: ", "").trim();
            if (!clientList.equals("None")) {
                String[] clients = clientList.split(", ");
                for (String client : clients) {
                    recipientListModel.addElement(client); // Add each client to the list
                }
            }

            // Reselect selected before item if it exists
            if (previouslySelected != null && recipientListModel.contains(previouslySelected)) {
                recipientList.setSelectedValue(previouslySelected, true);
            }
        });
    }

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 5555);
            new ClientGUI(socket);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Unable to connect to the server.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}