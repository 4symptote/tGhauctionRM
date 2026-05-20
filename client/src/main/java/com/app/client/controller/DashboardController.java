package com.app.client.controller;

import com.app.client.model.SessionModel;
import com.app.client.network.NetworkClient;
import com.app.client.network.ResponseListener;
import com.app.client.util.AuctionCardFactory;
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
                // Da cards
                HBox card = AuctionCardFactory.createCard(auction, () -> {
                    //System.out.println("Opening Auction: " + auction.getItem().getName());
                    NetworkClient.getInstance().removeListener(this);
                    SceneManager.getInstance().switchSceneWithData("/view/fxml/AuctionDetailView.fxml", auction);
                });
                card.prefWidthProperty().bind(auctionListContainer.widthProperty().multiply(0.9));
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
            switch (response.type()) {
                case AUCTION_UPDATED -> handleAuctionUpdatedResponse(response);
                case AUCTION_LIST     -> handleAuctionListResponse(response);
                // case BALANCE_UPDATED -> handleBalanceUpdateResponse(response);
                case USER_UPDATED    -> refreshAuctions();
            }
        });
    }

    private void handleAuctionListResponse(Response response) {
        if (response.success() && response.payload() instanceof List<?> rawList) {
            @SuppressWarnings("unchecked")
            List<Auction> auctions = (List<Auction>) rawList;
            updateAuctionList(auctions);
        }
    }

    private void handleAuctionUpdatedResponse(Response response) {
        refreshAuctions();
    }

    private void handleBalanceUpdateResponse(Response response) {
        if (response.success() && response.payload() instanceof Double balance) {
            if (SessionModel.getInstance().getCurrentUser() instanceof Bidder b) {
                b.setBalance(balance);
                balanceLabel.setText(String.format("Balance: $%,.2f", balance));
            }
        }
    }
}