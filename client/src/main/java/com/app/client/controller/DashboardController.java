package com.app.client.controller;

import com.app.client.model.SessionModel;
import com.app.client.network.NetworkClient;
import com.app.client.network.ResponseListener;
import com.app.client.util.SceneManager;
import com.app.shared.model.auction.Auction;
import com.app.shared.model.item.Art;
import com.app.shared.model.item.Electronics;
import com.app.shared.model.item.factory.ItemFactory;
import com.app.shared.model.user.Admin;
import com.app.shared.model.user.Bidder;
import com.app.shared.model.user.Seller;
import com.app.shared.model.user.User;
import com.app.shared.network.Response;
import com.app.shared.network.payload.CreateAuctionPayload;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class DashboardController implements ResponseListener {

    @FXML private Label welcomeLabel;
    @FXML private Label balanceLabel;
    @FXML private Label roleLabel;
    @FXML private VBox auctionListContainer; // The container from our FXML

    @FXML
    public void initialize() {
        NetworkClient.getInstance().addListener(this);

        // Load user data if available
        User currentUser = SessionModel.getInstance().getCurrentUser();
        if (currentUser != null) {
            welcomeLabel.setText(currentUser.getUsername());
            roleLabel.setText(currentUser.getClass().getSimpleName().toUpperCase());

            if (currentUser instanceof Bidder bidder) {
                balanceLabel.setText(String.format("Balance: $%,.2f", bidder.getBalance()));
            } else if (currentUser instanceof Seller seller) {
                balanceLabel.setText(String.format("Revenue: $%,.2f", seller.getTotalRevenue()));
            } else if (currentUser instanceof Admin admin) {
                balanceLabel.setText("System Administrator");
            }
        }

        // inject mock data
        injectMockCards();
    }

    private void injectMockCards() {
        List<Auction> mockAuctions = new ArrayList<>();

        // Mock 1: A running art auction
        Art painting = (Art) ItemFactory.createItem(new CreateAuctionPayload("Art", "Sunset", "nothing", 1000, "01249d091d0awd", 10000, null));
        Auction auction1 = new Auction(painting, 10000); // 24 hours

        mockAuctions.add(auction1);
        // Push to UI
        updateAuctionList(mockAuctions);
    }

    /**
     * Dynamically builds the horizontal UI cards for the given auctions.
     */
    public void updateAuctionList(List<Auction> auctions) {
        Platform.runLater(() -> {
            auctionListContainer.getChildren().clear();

            for (Auction auction : auctions) {
                // 1. The Main Card Container
                HBox card = new HBox(20);
                card.getStyleClass().add("auction-card");
                card.setAlignment(Pos.CENTER_LEFT);

                // FORCE THE CARD TO STRETCH TO FULL WIDTH
                card.setMaxWidth(Double.MAX_VALUE);

                // 2. Left Side: Name and Seller
                VBox infoBox = new VBox(8); // Slight spacing between title and seller
                HBox.setHgrow(infoBox, Priority.ALWAYS); // This pushes the right-side box all the way to the edge

                Label nameLabel = new Label(auction.getItem().getName());
                nameLabel.getStyleClass().add("card-item-name");

                Label sellerLabel = new Label("Seller: " + auction.getSellerId());
                sellerLabel.getStyleClass().add("card-seller-label");

                infoBox.getChildren().addAll(nameLabel, sellerLabel);

                // 3. Right Side: Price and Status Badge
                VBox priceBox = new VBox(10); // Spacing between price and badge
                priceBox.setAlignment(Pos.CENTER_RIGHT);

                Label priceLabel = new Label(String.format("$%,.2f", auction.getCurrentPrice()));
                priceLabel.getStyleClass().add("card-bid-value");

                Label statusLabel = new Label(auction.getStatus().name());
                statusLabel.getStyleClass().addAll("card-status-badge", "status-" + auction.getStatus().name());

                priceBox.getChildren().addAll(priceLabel, statusLabel);

                // 4. Combine and add to layout
                card.getChildren().addAll(infoBox, priceBox);

                // Add click effect
                card.setOnMouseClicked(e -> {
                    System.out.println("Card Clicked! Auction ID: " + auction.getId());
                    // Here you would navigate to the Auction Details View
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
    private void refreshAuctions(ActionEvent event) {
        System.out.println("Requesting updated auction list from server...");
        // NetworkClient.getInstance().sendRequest(new Request(Request.RequestType.GET_ALL_AUCTIONS, null));
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionModel.getInstance().logout();
        NetworkClient.getInstance().removeListener(this);
        SceneManager.getInstance().switchScene("/view/fxml/LoginView.fxml");
    }

    @Override
    public void onResponseReceived(Response response) {
        //updateAuctionList(new ArrayList<>());
    }
}