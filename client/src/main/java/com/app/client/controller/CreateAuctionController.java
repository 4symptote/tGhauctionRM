package com.app.client.controller;

import com.app.client.network.NetworkClient;
import com.app.client.network.ResponseListener;
import com.app.client.util.SceneManager;
import com.app.shared.network.Request;
import com.app.shared.network.Response;
import com.app.shared.network.payload.CreateAuctionPayload;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class CreateAuctionController implements ResponseListener {

    @FXML private TextField nameField;
    @FXML private TextArea descField;
    @FXML private TextField durationField;
    @FXML private TextField startingPriceField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        // Register this controller to listen to server responses
        NetworkClient.getInstance().addListener(this);
    }

    @FXML
    private void handleSubmit() {
        try {

            String name = nameField.getText().trim();
            String desc = descField.getText().trim();
            String type = typeComboBox.getValue();

            // validaition
            if (name.isEmpty() || type == null || durationField.getText().isEmpty() || startingPriceField.getText().isEmpty()) {
                errorLabel.setText("Please fill out all fields.");
                return;
            }

            // parsign
            double price = Double.parseDouble(startingPriceField.getText());
            long durationMillis = Long.parseLong(durationField.getText());

            // request
            CreateAuctionPayload payload = new CreateAuctionPayload(type, name, desc, price, "", durationMillis, null);
            Request request = new Request(Request.RequestType.CREATE_AUCTION, payload);

            errorLabel.setStyle("-fx-text-fill: #3498db;"); // Blue text for loading
            errorLabel.setText("Creating auction...");
            NetworkClient.getInstance().sendRequest(request);

        } catch (NumberFormatException e) {
            errorLabel.setStyle("-fx-text-fill: RED;");
            errorLabel.setText("Error: Price and Duration must be valid numbers.");
        }
    }

    @FXML
    private void handleCancel() {
        NetworkClient.getInstance().removeListener(this);
        SceneManager.getInstance().switchScene("/view/fxml/DashboardView.fxml");
    }

    @Override
    public void onResponseReceived(Response response) {
        Platform.runLater(() -> {
            if (response.success()) {
                System.out.println("Auction created successfully!");
                NetworkClient.getInstance().removeListener(this);
                SceneManager.getInstance().switchScene("/view/fxml/DashboardView.fxml");
            } else {
                errorLabel.setStyle("-fx-text-fill: RED;");
                errorLabel.setText(response.message()); // Show server error message
            }
        });
    }
}