package com.onevone.poker;

import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class CardView extends StackPane {

    static final double W = 80;
    static final double H = 110;

    public CardView(Card card) {
        String path = card.faceUp ? imagePath(card) : "/cards/BACK.png";
        Image image = new Image(CardView.class.getResourceAsStream(path), W, H, true, true);

        ImageView view = new ImageView(image);
        view.setFitWidth(W);
        view.setFitHeight(H);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setEffect(new DropShadow(6, 2, 2, Color.web("#00000066")));

        setMinSize(W, H);
        setMaxSize(W, H);
        getChildren().add(view);
    }

    private static String imagePath(Card card) {
        String rank = card.rank.label;
        String suit = switch (card.suit) {
            case CLUBS    -> "C";
            case DIAMONDS -> "D";
            case HEARTS   -> "H";
            case SPADES   -> "P";
        };
        return "/cards/" + rank + "-" + suit + ".png";
    }
}
