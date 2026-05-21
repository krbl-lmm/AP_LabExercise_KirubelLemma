package com.scribe.notepad;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

public class App extends Application {

    private File currentFile = null;
    private boolean darkMode = false;
    private boolean isEdited = false;

    @Override
    public void start(Stage stage) {
        TextArea textArea = new TextArea();
        textArea.setWrapText(true);

        //checking if edited
        textArea.textProperty().addListener((obs, oldText, newText) -> isEdited = true);

        //menu bar
        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu("File");
        Menu editMenu = new Menu("Edit");

        MenuItem newFile = new MenuItem("New");
        MenuItem openFile = new MenuItem("Open");
        MenuItem saveFile = new MenuItem("Save");
        MenuItem saveAsFile = new MenuItem("Save As");
        MenuItem exitFile = new MenuItem("Exit");

        MenuItem clearFile = new MenuItem("Clear");
        CheckMenuItem darkModeToggle = new CheckMenuItem("Dark Mode");

        fileMenu.getItems().addAll(newFile, new SeparatorMenuItem(), openFile, new SeparatorMenuItem(), saveFile, new SeparatorMenuItem(), saveAsFile, new SeparatorMenuItem(), exitFile);
        editMenu.getItems().addAll(clearFile, new SeparatorMenuItem(), darkModeToggle);

        menuBar.getMenus().addAll(fileMenu, editMenu);

        //file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Text File");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Text Files", "*.txt"), new FileChooser.ExtensionFilter("All Files", "*.*"));

        //stage layout
        BorderPane root = new BorderPane();
        root.setTop(menuBar);
        root.setCenter(textArea);

        Scene scene = new Scene(root, 800, 600);
        try {
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (NullPointerException e) {
            showError("Cannot find CSS file");
        }

        //button click events
        newFile.setOnAction(e -> {
            if (!askDiscard()) return;

            textArea.clear();
            currentFile = null;
            isEdited = false;
        });

        openFile.setOnAction(e -> {
            if (!askDiscard()) return;

            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                try {
                    textArea.setText(Files.readString(file.toPath()));
                    currentFile = file;
                    isEdited = false;
                } catch (IOException ex) {
                    showError("Error opening file");
                }
            }
        });

        saveFile.setOnAction(e -> {
            save(stage, textArea, fileChooser, false);
        });

        saveAsFile.setOnAction(e -> {
            save(stage, textArea, fileChooser, true);
        });

        clearFile.setOnAction(e -> {
            if (!askDiscard()) return;
            textArea.clear();
            isEdited = false;
        });

        exitFile.setOnAction(e -> {
            if(askDiscard()) stage.close();
        });

        darkModeToggle.setOnAction(e -> {
            darkMode = darkModeToggle.isSelected();
            applyTheme(root);
        });

        //showing stage
        stage.setTitle("Scribe Notepad");
        stage.setScene(scene);
        stage.show();
    }

    private void save(Stage stage, TextArea textArea, FileChooser chooser, boolean saveAs) {
        try {
            if (currentFile == null || saveAs) {
                File file = chooser.showSaveDialog(stage);
                if (file != null) currentFile = file;
            }
            if (currentFile != null) {
                Files.writeString(currentFile.toPath(), textArea.getText());
                isEdited = false;
            }
        } catch (IOException e) {
            showError("Error saving file");
        }
    }

    //alert that comes up if file isn't saved and you try to open another file
    private boolean askDiscard() {
        if (!isEdited) return true;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("You have unsaved changes.");
        alert.setContentText("Do you want to continue without saving?");

        ButtonType yes = new ButtonType("Discard");
        ButtonType no = new ButtonType("Cancel");

        alert.getButtonTypes().setAll(yes, no);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == yes;
    }

    //alert that shows error message
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void applyTheme(BorderPane root) {
        if (darkMode) {
            if (!root.getStyleClass().contains("dark")) {
                root.getStyleClass().add("dark");
            }
        } else {
            root.getStyleClass().remove("dark");
        }
    }

    public static void main(String[] args) {
        launch();
    }

}