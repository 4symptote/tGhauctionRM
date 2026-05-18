package com.app.client.util;

import com.app.shared.model.auction.Auction;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AuctionCardFactory {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm");

    public static HBox createCard(Auction auction, Runnable onClickAction) {
        HBox card = new HBox(20);
        card.getStyleClass().add("auction-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(1100.0);
        card.setMinWidth(450.0);

        // Left Side
        VBox infoBox = new VBox(8);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label nameLabel = new Label(auction.getItem().getName());
        nameLabel.getStyleClass().add("card-item-name");
        nameLabel.setWrapText(true);

        Label sellerLabel = new Label("Seller: " + auction.getSellerName());
        sellerLabel.getStyleClass().add("card-seller-label");

        Label timeLabel = new Label("Ends: " + sdf.format(new Date(auction.getEndTimeMillis())));
        timeLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 13px; -fx-font-weight: bold;");

        infoBox.getChildren().addAll(nameLabel, sellerLabel, timeLabel);

        // Right Side
        VBox priceBox = new VBox(10);
        priceBox.setAlignment(Pos.CENTER_RIGHT);

        Label priceLabel = new Label(String.format("$%,.2f", auction.getCurrentPrice()));
        priceLabel.getStyleClass().add("card-bid-value");

        Label statusLabel = new Label(auction.getStatus().name());
        statusLabel.getStyleClass().addAll("card-status-badge", "status-" + auction.getStatus().name());

        priceBox.getChildren().addAll(priceLabel, statusLabel);

        card.getChildren().addAll(infoBox, priceBox);

        // Bind the click action passed from the controller
        card.setOnMouseClicked(e -> onClickAction.run());

        return card;
    }
}