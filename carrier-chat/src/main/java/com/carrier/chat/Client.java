package com.carrier.chat;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client extends Application {
    private static final String serverAddress = "localhost";
    private static final int serverPort = 12345;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private ListView<String> messageList;
    private TextField inputField;
    private Button sendButton;
    private String userName;

    @Override
    public void start(Stage stage) {
        TextInputDialog nameDialog = new TextInputDialog("User");
        nameDialog.setTitle("Join Chat");
        nameDialog.setHeaderText("Welcome to Carrier Chat");
        nameDialog.setContentText("Enter your name:");

        userName = nameDialog.showAndWait().orElse("Anonymous");

        stage.setTitle("Carrier Chat - " + userName);

        buildUI(stage);

        connectToServer();
    }

    private void buildUI(Stage stage) {
        messageList = new ListView<>();
        VBox.setVgrow(messageList, Priority.ALWAYS);

        messageList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String message, boolean empty) {
                super.updateItem(message, empty);
                if (empty || message == null) {
                    setText(null);
                } else {
                    setText(message);
                }
            }
        });
        inputField = new TextField();
        inputField.setPromptText("Type a message...");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        inputField.setOnAction(e -> sendMessage());

        sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendMessage());

        HBox inputBar = new HBox(8, inputField, sendButton);
        inputBar.setAlignment(Pos.CENTER);
        inputBar.setPadding(new Insets(8));

        Label header = new Label("Carrier Chat  //  " + userName);
        header.setPadding(new Insets(10, 12, 10, 12));
        HBox headerBar = new HBox(header);

        VBox root = new VBox(headerBar, messageList, inputBar);

        Scene scene = new Scene(root, 600, 450);

        stage.setScene(scene);
        stage.show();

        inputField.requestFocus();
    }

    private void connectToServer() {
        Thread connectionThread = new Thread(() -> {
            try {
                socket = new Socket(serverAddress, serverPort);

                out = new PrintWriter(socket.getOutputStream(), true);
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                out.println(userName);

                Platform.runLater(() -> messageList.getItems().add("Connected to server as " + userName));

                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    final String displayMessage = serverMessage;

                    Platform.runLater(() -> {
                        messageList.getItems().add(displayMessage);

                        messageList.scrollTo(messageList.getItems().size() - 1);
                    });
                }
            } catch (IOException e) {
                Platform.runLater(() -> messageList.getItems().add("Could not connect to server: " + e.getMessage()));
            }
        });
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    private void sendMessage() {
        String message = inputField.getText().trim();

        if (message.isEmpty()) return;
        if (out == null) return;
        out.println(message);

        inputField.clear();
    }

    @Override
    public void stop() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing socket: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch();
    }
}