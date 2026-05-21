package com.scribe.notepad;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.FileChooser;

import java.io.File;

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


    }

    public static void main(String[] args) {
        launch();
    }

}