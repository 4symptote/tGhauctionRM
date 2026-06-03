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

import java.util.List;

public class WinningAuctionsController implements ResponseListener {

    @FXML private VBox auctionListContainer;
    @FXML private VBox mainContentVBox;

    @FXML
    public void initialize() {
        NetworkClient.getInstance().addListener(this);

        mainContentVBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                NetworkClient.getInstance().removeListener(this);
            }
        });

        mainContentVBox.widthProperty().addListener((obs, oldVal, newVal) -> {
            double currentWidth = newVal.doubleValue();
            double horizontalPadding = currentWidth * 0.10;
            horizontalPadding = Math.max(20, Math.min(100, horizontalPadding));
            mainContentVBox.setPadding(new javafx.geometry.Insets(30, horizontalPadding, 30, horizontalPadding));
        });

        refreshAuctions();
    }

    public void updateAuctionList(List<Auction> auctions) {
        // Sort: RUNNING first (active monitoring), then FINISHED (won), then others. Tie-break by time remaining.
        auctions.sort((a1, a2) -> {
            int priority1 = getWinningStatusPriority(a1.getStatus());
            int priority2 = getWinningStatusPriority(a2.getStatus());

            if (priority1 != priority2) {
                return Integer.compare(priority1, priority2);
            }
            return Long.compare(a1.getEndTimeMillis(), a2.getEndTimeMillis());
        });

        Platform.runLater(() -> {
            auctionListContainer.getChildren().clear();

            for (Auction auction : auctions) {
                HBox card = AuctionCardFactory.createCard(auction, () -> {
                    NetworkClient.getInstance().removeListener(this);
                    SceneManager.getInstance().switchSceneWithData("/view/fxml/AuctionDetailView.fxml", auction);
                });
                card.prefWidthProperty().bind(auctionListContainer.widthProperty().multiply(0.9));
                auctionListContainer.getChildren().add(card);
            }
        });
    }

    private int getWinningStatusPriority(Auction.Status status) {
        return switch (status) {
            case RUNNING -> 0;
            case OPEN -> 1;
            case FINISHED -> 2;
            default -> 3; // PAID, CANCELED
        };
    }

    @FXML
    private void refreshAuctions() {
        NetworkClient.getInstance().sendRequest(new Request(Request.RequestType.GET_WINNING_AUCTIONS, null));
    }

    @Override
    public void onResponseReceived(Response response) {
        Platform.runLater(() -> {
            switch (response.type()) {
                case AUCTION_UPDATED -> refreshAuctions();
                case WINNING_AUCTION_LIST -> {
                    if (response.success() && response.payload() instanceof List<?> rawList) {
                        @SuppressWarnings("unchecked")
                        List<Auction> auctions = (List<Auction>) rawList;
                        updateAuctionList(auctions);
                    }
                }
            }
        });
    }
}