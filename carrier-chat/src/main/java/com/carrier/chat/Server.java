package com.carrier.chat;

import java.net.*;
import java.io.*;
import java.util.*;

public class Server {
    private static final int port = 12345;

    private static final List<ClientHandler> connectedClients = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        System.out.println("Chat server starting on port " + port + "...");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is running. Waiting for clients...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                connectedClients.add(handler);

                Thread clientThread = new Thread(handler);
                clientThread.start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    public static void broadcast(String message, ClientHandler sender) {
        List<ClientHandler> snapshot = new ArrayList<>(connectedClients);

        for (ClientHandler client : snapshot) {
            client.sendMessage(message);
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                clientName = in.readLine();
                broadcast(clientName + " has joined the chat!", this);

                String message;
                while ((message = in.readLine()) != null) {
                    broadcast(clientName + ": " + message, this);
                }
            } catch (IOException e) {
                System.out.println(clientName + " disconnected.");
            } finally {
                cleanup();
            }
        }

        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        private void cleanup() {
            try {
                broadcast(clientName + " has left the chat.", this);
                connectedClients.remove(this);
                System.out.println("Client removed. Active clients remaining: " + connectedClients.size());
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.out.println("Error during cleanup: " + e.getMessage());
            }
        }
    }
}
