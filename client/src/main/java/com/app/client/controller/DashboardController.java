package com.app.client.controller;

import com.app.client.model.SessionModel;
import com.app.client.network.NetworkClient;
import com.app.client.network.ResponseListener;
import com.app.client.util.SceneManager;
import com.app.shared.model.auction.Auction;
import com.app.shared.model.user.Admin;
import com.app.shared.model.user.Bidder;
import com.app.shared.model.user.Seller;
import com.app.shared.model.user.User;
import com.app.shared.network.Request;
import com.app.shared.network.Response;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DashboardController implements ResponseListener {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
    @FXML private Label welcomeLabel;
    @FXML private Label balanceLabel;
    @FXML private Label roleLabel;
    @FXML private VBox auctionListContainer; // The container from our FXML
    @FXML private VBox mainContentVBox;

    @FXML
    public void initialize() {
        NetworkClient.getInstance().addListener(this);

        mainContentVBox.widthProperty().addListener((obs, oldVal, newVal) -> {
            double currentWidth = newVal.doubleValue();
            double horizontalPadding = currentWidth * 0.12;

            horizontalPadding = Math.max(40, Math.min(250, horizontalPadding));
            mainContentVBox.setPadding(new javafx.geometry.Insets(30, horizontalPadding, 30, horizontalPadding));
        });

        // Load user data if available
        User currentUser = SessionModel.getInstance().getCurrentUser();
        if (currentUser != null) {
            welcomeLabel.setText(currentUser.getUsername());
            roleLabel.setText(currentUser.getClass().getSimpleName().toUpperCase());

            switch (currentUser) {
                case Bidder bidder -> balanceLabel.setText(String.format("Balance: $%,.2f", bidder.getBalance()));
                case Seller seller -> balanceLabel.setText(String.format("Revenue: $%,.2f", seller.getTotalRevenue()));
                case Admin admin -> balanceLabel.setText("System Administrator");
                default -> {
                }
            }
        }
        refreshAuctions();

    }

    public void updateAuctionList(List<Auction> auctions) {
        Platform.runLater(() -> {
            auctionListContainer.getChildren().clear();

            for (Auction auction : auctions) {
                // 1. The Main Card Container
                HBox card = new HBox(20);
                card.getStyleClass().add("auction-card");
                card.setAlignment(Pos.CENTER_LEFT);

                card.prefWidthProperty().bind(auctionListContainer.widthProperty().multiply(0.95));
                card.setMaxWidth(1100.0);
                card.setMinWidth(450.0);

                // Left Side: Name and Seller
                VBox infoBox = new VBox(8); // Slight spacing between title and seller
                HBox.setHgrow(infoBox, Priority.ALWAYS); // This pushes the right-side box all the way to the edge

                Label nameLabel = new Label(auction.getItem().getName());
                nameLabel.getStyleClass().add("card-item-name");
                nameLabel.setWrapText(true);


                Label sellerLabel = new Label("Seller: " + auction.getSellerName());
                sellerLabel.getStyleClass().add("card-seller-label");

                infoBox.getChildren().addAll(nameLabel, sellerLabel);

                // Right Side: Price and Status Badge
                VBox priceBox = new VBox(10); // Spacing between price and badge
                priceBox.setAlignment(Pos.CENTER_RIGHT);

                Label priceLabel = new Label(String.format("$%,.2f", auction.getCurrentPrice()));
                priceLabel.getStyleClass().add("card-bid-value");

                Label statusLabel = new Label(auction.getStatus().name());
                statusLabel.getStyleClass().addAll("card-status-badge", "status-" + auction.getStatus().name());

                priceBox.getChildren().addAll(priceLabel, statusLabel);

                // Combine and add to layout
                card.getChildren().addAll(infoBox, priceBox);

                // Add click effect
                card.setOnMouseClicked(e -> {
                    System.out.println("Opening Auction: " + auction.getItem().getName());
                    // Unsubscribe from network events to prevent memory leaks
                    NetworkClient.getInstance().removeListener(this);
                    // Switch scene and pass the clicked auction!
                    SceneManager.getInstance().switchSceneWithData("/view/fxml/AuctionDetailView.fxml", auction);
                });

                auctionListContainer.getChildren().add(card);
            }
        });
    }

    @FXML
    private void handleCreateAuction(ActionEvent event) {
        SceneManager.getInstance().switchScene("/view/fxml/CreateAuctionView.fxml");
    }

    @FXML
    private void refreshAuctions() {
        //System.out.println("Requesting updated auction list from server...");
        NetworkClient.getInstance().sendRequest(new Request(Request.RequestType.GET_AUCTIONS, null));
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        NetworkClient.getInstance().sendRequest(new Request(Request.RequestType.LOGOUT, null));
        SessionModel.getInstance().logout();
        NetworkClient.getInstance().removeListener(this);
        SceneManager.getInstance().switchScene("/view/fxml/LoginView.fxml");
    }

    @Override
    public void onResponseReceived(Response response) {
        // veri important Platform.runLater
        Platform.runLater(() -> {
            if (response.type() == Response.ResponseType.AUCTION_LIST) {
                try {
                    // 100% sure that the payload is a List<Auction>
                    @SuppressWarnings("unchecked")
                    List<Auction> fetchedAuctions = (List<Auction>) response.payload();
                    // update UI
                    updateAuctionList(fetchedAuctions);

                } catch (ClassCastException e) {
                    log.error("Error casting payload to List<Auction>: {}", e.getMessage());
                }
            } else if (response.type() == Response.ResponseType.AUCTION_UPDATED) {
                refreshAuctions();
            }
        });
    }
}