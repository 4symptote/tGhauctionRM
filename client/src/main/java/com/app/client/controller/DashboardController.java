package com.app.client.controller;

import com.app.client.network.NetworkClient;
import com.app.client.network.ResponseListener;
import com.app.client.util.AuctionCardFactory;
import com.app.client.util.SceneManager;
import com.app.shared.model.auction.Auction;
import com.app.shared.network.Request;
import com.app.shared.network.Response;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DashboardController implements ResponseListener {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    @FXML private VBox auctionListContainer; // The container from our FXML
    @FXML private VBox mainContentVBox;

    @FXML
    public void initialize() {
        NetworkClient.getInstance().addListener(this);

        mainContentVBox.widthProperty().addListener((obs, oldVal, newVal) -> {
            double currentWidth = newVal.doubleValue();
            double horizontalPadding = currentWidth * 0.07;

            horizontalPadding = Math.max(20, Math.min(100, horizontalPadding));
            mainContentVBox.setPadding(new javafx.geometry.Insets(30, horizontalPadding, 30, horizontalPadding));
        });

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
    private void refreshAuctions() {
        //System.out.println("Requesting updated auction list from server...");
        NetworkClient.getInstance().sendRequest(new Request(Request.RequestType.GET_AUCTIONS, null));
    }

    @Override
    public void onResponseReceived(Response response) {
        // veri important Platform.runLater
        Platform.runLater(() -> {
            switch (response.type()) {
                case AUCTION_UPDATED -> handleAuctionUpdatedResponse(response);
                case AUCTION_LIST    -> handleAuctionListResponse(response);
                //case USER_UPDATED    -> handleUserUpdateResponse(response);
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

}