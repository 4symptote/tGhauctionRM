package com.app.client.controller;

import com.app.client.model.SessionModel;
import com.app.client.network.NetworkClient;
import com.app.client.network.ResponseListener;
import com.app.client.util.SceneManager;
import com.app.shared.model.auction.Auction;
import com.app.shared.model.item.Art;
import com.app.shared.model.item.Electronics;
import com.app.shared.model.item.Item;
import com.app.shared.model.item.Vehicle;
import com.app.shared.network.Request;
import com.app.shared.network.Response;
import com.app.shared.network.payload.BidPayload;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class AuctionDetailController implements ResponseListener {

    @FXML private VBox mainContentVBox;
    @FXML private Label statusBadge;
    @FXML private Label itemNameLabel;
    @FXML private Label sellerLabel;
    @FXML private Label descriptionLabel;
    @FXML private VBox specsContainer;
    @FXML private Label currentPriceLabel;
    @FXML private Label highestBidderLabel;
    @FXML private TextField bidAmountField;
    @FXML private Label bidMessageLabel;

    private Auction currentAuction;

    @FXML
    public void initialize() {
        NetworkClient.getInstance().addListener(this);

        // Dynamic padding for responsiveness, just like the dashboard
        mainContentVBox.widthProperty().addListener((obs, oldVal, newVal) -> {
            double padding = Math.max(40, Math.min(200, newVal.doubleValue() * 0.10));
            mainContentVBox.setPadding(new javafx.geometry.Insets(40, padding, 50, padding));
        });
    }


    public void initData(Auction auction) {
        this.currentAuction = auction;
        populateUI();
    }

    private void populateUI() {
        if (currentAuction == null) return;

        Item item = currentAuction.getItem();

        // Basic Info
        itemNameLabel.setText(item.getName());
        sellerLabel.setText("Listed by: " + currentAuction.getSellerName());
        descriptionLabel.setText(item.getDescription());

        // Pricing & Status
        currentPriceLabel.setText(String.format("$%,.2f", currentAuction.getCurrentPrice()));

        statusBadge.setText(currentAuction.getStatus().name());
        statusBadge.getStyleClass().removeAll("status-OPEN", "status-RUNNING", "status-FINISHED");
        statusBadge.getStyleClass().add("status-" + currentAuction.getStatus().name());

        if (currentAuction.getHighestBidderId() != null) {
            highestBidderLabel.setText("Highest Bidder ID: " + currentAuction.getHighestBidderId());
        } else {
            highestBidderLabel.setText("No bids placed yet.");
        }

        // custom attributes
        // todo: idk -make it expandable?
        specsContainer.getChildren().clear(); // reset
        addSpecRow("Starting Price", String.format("$%,.2f", item.getStartingPrice()));

        switch (item) {
            case Electronics elec -> addSpecRow("Brand", elec.getBrand());
            case Vehicle vehicle -> {
                addSpecRow("Brand", vehicle.getBrand());
                addSpecRow("Model", vehicle.getModel());
            }
            case Art art -> {
                addSpecRow("Artist", art.getArtist());
                addSpecRow("Medium", art.getMedium());
                addSpecRow("Year", String.valueOf(art.getYear()));
            }
            default -> {
            }
        }
    }

    private void addSpecRow(String labelText, String valueText) {
        Label specLabel = new Label(labelText + ": ");
        specLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #7f8c8d;");

        Label valLabel = new Label(valueText != null ? valueText : "N/A");
        valLabel.setStyle("-fx-text-fill: #2c3e50;");

        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(5, specLabel, valLabel);
        specsContainer.getChildren().add(row);
    }

    @FXML
    private void handlePlaceBid(ActionEvent event) {
        try {
            double amount = Double.parseDouble(bidAmountField.getText().trim());

            bidMessageLabel.setStyle("-fx-text-fill: #3498db;");
            bidMessageLabel.setText("Submitting bid...");

            BidPayload payload = new BidPayload(currentAuction.getId(), amount);
            NetworkClient.getInstance().sendRequest(new Request(Request.RequestType.PLACE_BID, payload));

        } catch (NumberFormatException e) {
            bidMessageLabel.setStyle("-fx-text-fill: RED;");
            bidMessageLabel.setText("Please enter a valid amount.");
        }
    }

    @FXML
    private void handleBackToDashboard(ActionEvent event) {
        NetworkClient.getInstance().removeListener(this);
        SceneManager.getInstance().switchScene("/view/fxml/DashboardView.fxml");
    }

    @Override
    public void onResponseReceived(Response response) {
        Platform.runLater(() -> {
            if (response.success()) {
                bidMessageLabel.setStyle("-fx-text-fill: #27ae60;");
                bidMessageLabel.setText("Bid placed successfully!");
                bidAmountField.clear();

                // todo: handle response and update ui
                if (response.payload() instanceof Auction updatedAuction) {
                    this.currentAuction = updatedAuction;
                    populateUI();
                }

            } else {
                bidMessageLabel.setStyle("-fx-text-fill: RED;");
                bidMessageLabel.setText(response.message());
            }
        });
    }
}