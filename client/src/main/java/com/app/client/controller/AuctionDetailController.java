package com.app.client.controller;

import com.app.client.model.SessionModel;
import com.app.client.network.NetworkClient;
import com.app.client.network.ResponseListener;
import com.app.client.util.SceneManager;
import com.app.client.util.TimeUtil;
import com.app.shared.model.auction.Auction;
import com.app.shared.model.auction.BidTransaction;
import com.app.shared.model.item.Art;
import com.app.shared.model.item.Electronics;
import com.app.shared.model.item.Item;
import com.app.shared.model.item.Vehicle;
import com.app.shared.model.user.User;
import com.app.shared.network.Request;
import com.app.shared.network.Response;
import com.app.shared.network.payload.AutoBidPayload;
import com.app.shared.network.payload.BidPayload;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class AuctionDetailController implements ResponseListener {

    @FXML
    private VBox mainContentVBox;
    @FXML
    private Label statusBadge;
    @FXML
    private Label itemNameLabel;
    @FXML
    private Label sellerLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private VBox specsContainer;
    @FXML
    private Label currentPriceLabel;
    @FXML
    private Label highestBidderLabel;
    @FXML
    private TextField bidAmountField;
    @FXML
    private Label bidMessageLabel;

    @FXML
    private VBox autoBidContainer;
    @FXML
    private TextField autoBidLimitField;

    @FXML
    private LineChart<String, Number> priceChart;
    @FXML
    private TableView<BidTransaction> bidHistoryTable;
    @FXML
    private TableColumn<BidTransaction, String> timeCol;
    @FXML
    private TableColumn<BidTransaction, String> amountCol;
    @FXML
    private TableColumn<BidTransaction, String> bidderCol;

    @FXML
    private Label endTimeLabel;        // NEW: Shows the static date
    @FXML
    private Label timeRemainingLabel;  //  clock

    private AnimationTimer countdownTimer;

    private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm:ss");

    private Auction currentAuction;

    @FXML
    public void initialize() {
        NetworkClient.getInstance().addListener(this);

        mainContentVBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null && countdownTimer != null) {
                countdownTimer.stop();
                NetworkClient.getInstance().removeListener(this);
            }
        });

        mainContentVBox.widthProperty().addListener((obs, oldVal, newVal) -> {
            double padding = Math.max(40, Math.min(200, newVal.doubleValue() * 0.10));
            mainContentVBox.setPadding(new javafx.geometry.Insets(40, padding, 50, padding));
        });


        timeCol.setCellValueFactory(data -> new SimpleStringProperty(sdf.format(new Date(data.getValue().timestamp()))));
        amountCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("$%,.2f", data.getValue().amount())));
        bidderCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().bidderName()));
    }


    public void initData(Auction auction) {
        this.currentAuction = auction;
        populateUI();

        NetworkClient.getInstance().sendRequest(new Request(Request.RequestType.GET_BID_HISTORY, auction.getId()));

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
            String bName = currentAuction.getHighestBidderName();
            highestBidderLabel.setText("Highest Bidder: " + (bName != null ? bName : "Unknown"));
        } else {
            highestBidderLabel.setText("No bids placed yet.");
        }
        //
        // Ẩn autobid nếu k phải bidder
        User currentUser = SessionModel.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.canBid() && currentAuction.getStatus() == Auction.Status.RUNNING) {
            autoBidContainer.setVisible(true);
            autoBidContainer.setManaged(true);
        } else {
            autoBidContainer.setVisible(false);
            autoBidContainer.setManaged(false);
        }

        endTimeLabel.setText("Ends: " + TimeUtil.formatExactDate(currentAuction.getEndTimeMillis()));
        countdownTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                String countdownStr = TimeUtil.formatCountdown(currentAuction.getEndTimeMillis());

                if ("Ended".equals(countdownStr)) {
                    timeRemainingLabel.setText("Status: Ended");
                    timeRemainingLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 16px;");
                    stop();
                } else {
                    timeRemainingLabel.setText("Time Left: " + countdownStr);
                    timeRemainingLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold; -fx-font-size: 16px;");
                }
            }
        };
        countdownTimer.start();

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
    private void handleSetAutoBid() {
        String limitStr = autoBidLimitField.getText();
        if (limitStr == null || limitStr.trim().isEmpty()) {
            showAlert("Invalid Input", "Please enter a maximum limit.");
            return;
        }

        try {
            double maxLimit = Double.parseDouble(limitStr);

            // Basic validation
            if (maxLimit <= currentAuction.getCurrentPrice()) {
                showAlert("Invalid Limit", "Your auto-bid limit must be higher than the current price!");
                return;
            }

            // Create payload and send to the engine!
            AutoBidPayload payload = new AutoBidPayload(currentAuction.getId(), maxLimit);

            Request request = new Request(Request.RequestType.SET_AUTO_BID, payload);

            NetworkClient.getInstance().sendRequest(request);

        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter a valid numeric amount.");
        }
    }

    private void updateHistoryUI(List<BidTransaction> history) {
        // Populate Table
        bidHistoryTable.setItems(FXCollections.observableArrayList(history));

        // Populate Chart
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Price Climb");

        // Add the starting price as point 0
        series.getData().add(new XYChart.Data<>("Start", currentAuction.getItem().getStartingPrice()));

        for (BidTransaction bid : history) {
            String time = new SimpleDateFormat("dd HH:mm:ss").format(new Date(bid.timestamp()));
            series.getData().add(new XYChart.Data<>(time, bid.amount()));
        }

        priceChart.getData().clear();
        priceChart.getData().add(series);
    }

    @Override
    public void onResponseReceived(Response response) {
        Platform.runLater(() -> {
            switch (response.type()) {
                case PLACED_BID -> handlePlacedBidResponse(response);
                case AUCTION_UPDATED -> handleAuctionBroadcast(response);
                case BID_HISTORY -> handleHistoryResponse(response);
                case AUTO_BID_SET -> handleUserUpdatedResponse(response);
            }
        });
    }


    //  RESPONSE HANDLERS
    // PLACE_BID
    private void handlePlacedBidResponse(Response response) {
        if (response.success()) {
            bidMessageLabel.setStyle("-fx-text-fill: #27ae60;");
            bidMessageLabel.setText("Bid placed successfully!");
            bidAmountField.clear();
        } else {
            bidMessageLabel.setStyle("-fx-text-fill: RED;");
            bidMessageLabel.setText(response.message());
        }
    }

    // AUCTION_UPDATED
    private void handleAuctionBroadcast(Response response) {
        if (response.success() && response.payload() instanceof Auction updatedAuction) {
            if (this.currentAuction != null && this.currentAuction.getId().equals(updatedAuction.getId())) {
                this.currentAuction = updatedAuction;
                populateUI();
                NetworkClient.getInstance().sendRequest(new Request(Request.RequestType.GET_BID_HISTORY, currentAuction.getId()));
            }
        }
    }

    // BID_HISTORY
    private void handleHistoryResponse(Response response) {
        if (response.success() && response.payload() instanceof List<?> rawList) {
            @SuppressWarnings("unchecked")
            List<BidTransaction> history = (List<BidTransaction>) rawList; // cast to List<BidTransaction>
            updateHistoryUI(history);
        }
    }

    //AUTO+BID_SET
    private void handleUserUpdatedResponse(Response response) {
        if (response.success() && response.payload() instanceof User updatedUser) {
            SessionModel.getInstance().setCurrentUser(updatedUser);


            showAlert("Auto-Bid Active", response.message());
            autoBidLimitField.clear();

        } else {
            showAlert("Failed", response.message());
        }
    }


    // temporal showAlrt
    private void showAlert(String title, String content) {
        javafx.application.Platform.runLater(() -> {
            Alert.AlertType type = Alert.AlertType.INFORMATION;

            if (title.toLowerCase().contains("error") || title.toLowerCase().contains("failed") || title.toLowerCase().contains("invalid")) {
                type = Alert.AlertType.ERROR;
            }

            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null); // Removes the awkward extra header space
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

}