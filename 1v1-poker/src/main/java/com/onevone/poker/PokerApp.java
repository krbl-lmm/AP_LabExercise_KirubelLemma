package com.onevone.poker;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

public class PokerApp extends Application {

    private final PokerGame game = new PokerGame();

    private final Label lblMessage      = styledLabel("Press 'New Hand' to start.", 15, "#e0e0e0");
    private final Label lblPot          = styledLabel("Pot: $0", 16, "#ffd700");
    private final Label lblPlayerChips  = styledLabel("Your chips: $1000", 14, "#aaffaa");
    private final Label lblComputerChips= styledLabel("Computer: $1000", 14, "#ffaaaa");
    private final Label lblPhase        = styledLabel("", 13, "#aaaaaa");
    private final Label lblRaiseAmount  = styledLabel("Raise: $20", 12, "#ffd700");

    private final HBox computerCardRow  = cardRow();
    private final HBox communityCardRow = cardRow();
    private final HBox playerCardRow    = cardRow();

    private final Button btnNewHand = button("New Hand",  "#2244aa");
    private PauseTransition pendingAI = null; // track so we can cancel it on new hand
    private final Button btnFold    = button("Fold",      "#8b0000");
    private final Button btnCheck   = button("Check",     "#005500");
    private final Button btnCall    = button("Call",      "#006600");
    private final Button btnRaise   = button("Raise",     "#884400");
    private final Slider raiseSlider = makeSlider();

    @Override
    public void start(Stage stage) {
        stage.setTitle("Texas Hold'em — 1 vs Computer");

        VBox root = new VBox(12);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #0a1628;");

        VBox computerSection = new VBox(6, lblComputerChips, computerCardRow);
        computerSection.setAlignment(Pos.CENTER);

        StackPane table = buildTable();

        VBox playerSection = new VBox(6, playerCardRow, lblPlayerChips);
        playerSection.setAlignment(Pos.CENTER);

        HBox actionButtons = new HBox(10, btnFold, btnCheck, btnCall, btnRaise);
        actionButtons.setAlignment(Pos.CENTER);

        HBox raiseRow = new HBox(10, raiseSlider, lblRaiseAmount);
        raiseRow.setAlignment(Pos.CENTER);

        VBox controls = new VBox(8, lblMessage, actionButtons, raiseRow, btnNewHand);
        controls.setAlignment(Pos.CENTER);

        root.getChildren().addAll(computerSection, table, playerSection, controls);

        btnNewHand.setOnAction(e -> {
            if (pendingAI != null) { pendingAI.stop(); pendingAI = null; }
            game.startHand();
            updateUI();
            triggerComputerIfNeeded();
        });
        btnFold   .setOnAction(e -> { game.playerFold();           updateUI(); });
        btnCheck  .setOnAction(e -> { game.playerCheck();          updateUI(); triggerComputerIfNeeded(); });
        btnCall   .setOnAction(e -> { game.playerCall();           updateUI(); triggerComputerIfNeeded(); });
        btnRaise  .setOnAction(e -> {
            int amount = (int) raiseSlider.getValue();
            game.playerRaise(amount);
            updateUI();
            triggerComputerIfNeeded();
        });

        raiseSlider.valueProperty().addListener((obs, old, val) ->
            lblRaiseAmount.setText("Raise: $" + val.intValue()));

        Scene scene = new Scene(root, 700, 680);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        updateUI();
    }

    private StackPane buildTable() {
        Ellipse outerRail = new Ellipse(310, 120);
        outerRail.setFill(Color.web("#3b2007"));
        outerRail.setEffect(new DropShadow(10, Color.BLACK));

        Ellipse felt = new Ellipse(290, 105);
        felt.setFill(Color.web("#1a5c2a"));

        VBox tableContent = new VBox(6, lblPhase, communityCardRow, lblPot);
        tableContent.setAlignment(Pos.CENTER);

        return new StackPane(outerRail, felt, tableContent);
    }

    private void updateUI() {
        PokerGame.Phase phase = game.getPhase();
        boolean isShowdown = phase == PokerGame.Phase.SHOWDOWN;
        boolean isWaiting  = phase == PokerGame.Phase.WAITING;
        boolean playerTurn = game.isPlayerTurn();

        lblPlayerChips  .setText("Your chips: $"    + game.getPlayerChips());
        lblComputerChips.setText("Computer: $"      + game.getComputerChips());
        lblPot          .setText("Pot: $"           + game.getPot());
        lblPhase        .setText(phase.name().replace("_", " "));
        lblMessage      .setText(game.getLastMessage());

        raiseSlider.setMax(Math.max(20, game.getPlayerChips()));

        refreshCardRow(computerCardRow, game.getComputerCards());
        refreshCardRow(communityCardRow, game.getCommunity());
        refreshCardRow(playerCardRow,   game.getPlayerCards());

        boolean canAct = playerTurn && !isShowdown && !isWaiting;
        btnFold .setDisable(!canAct);
        btnRaise.setDisable(!canAct);
        btnCheck.setDisable(!canAct || !game.canCheck());
        btnCall .setDisable(!canAct || game.canCheck());
        btnNewHand.setDisable(!isShowdown && !isWaiting);

        if (game.callAmount() > 0)
            btnCall.setText("Call $" + game.callAmount());
        else
            btnCall.setText("Call");

        if (isShowdown) {
            if (game.getPlayerResult() != null)
                lblPlayerChips.setText("Your chips: $" + game.getPlayerChips()
                    + "  [" + game.getPlayerResult().describe() + "]");
            if (game.getComputerResult() != null)
                lblComputerChips.setText("Computer: $" + game.getComputerChips()
                    + "  [" + game.getComputerResult().describe() + "]");
        }
    }

    private void refreshCardRow(HBox row, java.util.List<Card> cards) {
        row.getChildren().clear();
        for (Card c : cards) row.getChildren().add(new CardView(c));
    }

    private void triggerComputerIfNeeded() {
        if (!game.isComputerTurn()) return;
        pendingAI = new PauseTransition(Duration.millis(800));
        pendingAI.setOnFinished(e -> {
            game.doComputerAction();
            updateUI();
            triggerComputerIfNeeded();
        });
        pendingAI.play();
    }

    private static Label styledLabel(String text, int size, String color) {
        Label l = new Label(text);
        l.setFont(Font.font("Georgia", FontWeight.BOLD, size));
        l.setStyle("-fx-text-fill: " + color + ";");
        return l;
    }

    private static Button button(String text, String bgColor) {
        Button b = new Button(text);
        b.setPrefWidth(90);
        b.setPrefHeight(36);
        b.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        b.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: white; "
                + "-fx-background-radius: 7; -fx-cursor: hand;");
        b.setEffect(new DropShadow(4, 1, 2, Color.web("#00000077")));
        return b;
    }

    private static HBox cardRow() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER);
        row.setMinHeight(CardView.H + 4);
        return row;
    }

    private static Slider makeSlider() {
        Slider s = new Slider(20, 1000, 40);
        s.setPrefWidth(200);
        s.setBlockIncrement(20);
        s.setMajorTickUnit(100);
        s.setSnapToTicks(false);
        return s;
    }

    public static void main(String[] args) { launch(args); }
}
