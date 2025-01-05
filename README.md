📡 Chat Application with GUI: Client-Server Implementation

Welcome to the Chat Application repository! This project implements a client-server chat application with GUI interfaces for both the server and clients. The application supports multiple clients, private messaging, banned phrases, and user management features.

📝 Features

🌐 Client GUI
	•	Chat Area: Displays all received messages.
	•	Message Input: Allows typing and sending messages.
	•	Recipient Selection: Choose to send messages to specific clients or everyone.
	•	List Clients: View a list of connected clients.
	•	Banned Phrases: Retrieve a list of banned phrases from the server.

🖥️ Server GUI
	•	Logs Panel: Displays server events, like client connections and disconnections.
	•	Connected Clients: Displays a live list of connected clients.

🛠️ Server Functionality
	•	Manages client connections.
	•	Maintains and shares the list of connected clients.
	•	Filters messages containing banned phrases.
	•	Broadcasts messages to all clients or routes them to specific recipients.
	•	Handles commands like /clients and /banned.

🏗️ Architecture
	1.	Server:
	•	Hosts the chat room and processes client requests.
	•	Reads configurations from config.json, including:
	•	Server name
	•	Port
	•	Banned phrases
	•	Manages client handlers using a thread-per-client model.
	2.	Client:
	•	Connects to the server using a socket.
	•	Sends and receives messages or commands via a GUI.
	3.	Communication:
	•	Messages are routed via sockets.
	•	Commands like /clients and /banned are parsed and processed.

🚀 Getting Started

Prerequisites
	•	Java 8+ installed
 
Clone the Repository

git clone git@github.com:JakubGralinski/Java-UTP.git
cd Java-UTP

⚙️ Configuration

Modify src/config.json to set up the server:

{
  "port": 5555,
  "serverName": "Chat Server",
  "bannedPhrases": ["spam", "offensive"],
  "numberOfClients": 10
}

Build and Run

1️⃣ Start the Server

cd src
javac Server.java ServerGUI.java
java ServerGUI

2️⃣ Start a Client

javac ClientGUI.java
java ClientGUI

You can run multiple instances of the client to simulate multiple users.

🛡️ Usage

Server
	•	Logs Panel: Monitors server events in real-time.
	•	Client List Panel: Displays currently connected users.

Client
	•	Send Message: Enter a message and click Send.
	•	Private Message: Select recipients from the list and type your message.
	•	Commands:
	•	/clients: Displays the list of connected clients.
	•	/banned: Displays the list of banned phrases.

🛠️ Development Notes

Modules
	•	ClientGUI.java: Handles the client-side GUI and communication.
	•	ServerGUI.java: Manages the server-side GUI and logs.
	•	Server.java: Implements the server’s core logic, client management, and broadcasting.
	•	ClientHandler.java: Handles individual client connections.
	•	config.json: Stores server configurations.

Key Features
	•	Thread Management: Each client runs on its own thread.
	•	Dynamic Client List: Updates client lists in real-time across all clients.
	•	Command Parsing: Recognizes commands (/clients, /banned) sent by clients.
	•	Error Handling: Handles invalid messages, disconnects, and duplicate nicknames.

📂 Folder Structure

Java-UTP/
│
├── src/
│   ├── ClientGUI.java
│   ├── ClientHandler.java
│   ├── Server.java
│   ├── ServerGUI.java
│   ├── config.json
│
└── README.md

🌟 Highlights
	•	Real-time Communication: Enables seamless messaging between multiple clients.
	•	Scalability: Easily extendable for additional features.
	•	Intuitive GUIs: Simple and user-friendly interface for both server and clients.
	•	Customizable: Modify config.json for personalized settings.

🛡️ Security Notes
	•	Messages with banned phrases are filtered out.
	•	Unique nicknames ensure no two clients have the same identifier.
	•	Error messages notify users when sending to nonexistent recipients.

💡 Future Enhancements
	•	Encryption: Add SSL for secure communication.
	•	User Authentication: Implement login functionality.
	•	File Sharing: Allow users to exchange files.
	•	Chat Rooms: Add support for multiple chat rooms.

✨ Contributors
	•	Jakub Gralinski (Developer and Maintainer)

🤝 Contributing

Feel free to fork this repository and submit pull requests for improvements or bug fixes.

📜 License

This project is licensed under the MIT License. See the LICENSE file for details.

🎉 Happy Chatting!
