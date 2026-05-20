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
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

public class CreateAuctionController implements ResponseListener {

    @FXML private TextField nameField;
    @FXML private TextArea descField;
    @FXML private TextField durationField;
    @FXML private TextField startingPriceField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private Label errorLabel;
    @FXML private VBox dynamicAttributesContainer;
    // todo: sau nay se la chon thoi gian cu the, d/h/m/s
    @FXML private TextField startDelayMinutesField;

    private final Map<String, TextField> dynamicFieldsMap = new HashMap<>();

    @FXML
    public void initialize() {
        // Register this controller to listen to server responses
        NetworkClient.getInstance().addListener(this);

        typeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateDynamicFields(newVal);
        });
    }

    private void updateDynamicFields(String categoryType) {
        dynamicAttributesContainer.getChildren().clear();
        dynamicFieldsMap.clear();

        if (categoryType == null) return;

        switch (categoryType) {
            case "Electronics" -> {
                addDynamicField("Brand", "brand");
            }
            case "Art" -> {
                addDynamicField("Artist Name", "artist");
                addDynamicField("Medium (e.g., Oil, Watercolor)", "medium");
                addDynamicField("Year Created", "year");
            }
            case "Vehicle" -> {
                addDynamicField("Brand", "brand");
                addDynamicField("Model", "model");
            }
        }
    }
    private void addDynamicField(String labelText, String payloadKey) {
        VBox row = new VBox(5);
        Label label = new Label(labelText);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e; -fx-font-size: 12px;");

        TextField textField = new TextField();
        textField.setStyle("-fx-padding: 8; -fx-background-color: #f8f9fa; -fx-border-color: #bdc3c7; -fx-border-radius: 4;");

        row.getChildren().addAll(label, textField);
        dynamicAttributesContainer.getChildren().add(row);

        // check ItemFactory
        dynamicFieldsMap.put(payloadKey, textField);
    }

    @FXML
    private void handleSubmit() {
        try {
            String name = nameField.getText().trim();
            String desc = descField.getText().trim();
            String type = typeComboBox.getValue();

            // validation
            if (name.isEmpty() || type == null || durationField.getText().isEmpty() || startingPriceField.getText().isEmpty()) {
                errorLabel.setText("Please fill out all fields.");
                return;
            }

            // parsing
            double price = Double.parseDouble(startingPriceField.getText());
            long durationMillis = Long.parseLong(durationField.getText());

            long startDelayMillis ;
            if (startDelayMinutesField != null && !startDelayMinutesField.getText().trim().isEmpty()) {
                long secondsDelay = Long.parseLong(startDelayMinutesField.getText().trim());
                startDelayMillis = secondsDelay * 1000;
            } else {
                startDelayMillis = 0;
            }
            long calculatedStartTime = System.currentTimeMillis() + startDelayMillis;

            Map<String, Object> customAttributes = getCustomAttributes();

            // request
            CreateAuctionPayload payload = new CreateAuctionPayload(type, name, desc, price, calculatedStartTime, durationMillis, customAttributes);
            Request request = new Request(Request.RequestType.CREATE_AUCTION, payload);

            errorLabel.setStyle("-fx-text-fill: #3498db;"); // Blue text for loading
            errorLabel.setText("Creating auction...");
            NetworkClient.getInstance().sendRequest(request);

        } catch (NumberFormatException e) {
            errorLabel.setStyle("-fx-text-fill: RED;");
            errorLabel.setText("Error: Price and Duration must be valid numbers.");
        }
    }

    private Map<String, Object> getCustomAttributes() {
        Map<String, Object> customAttributes = new HashMap<>();
        for (Map.Entry<String, TextField> entry : dynamicFieldsMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().getText().trim();

            // ...
            if (!value.isEmpty()) {
                if (key.equals("year") || key.equals("mileage")) {
                    customAttributes.put(key, Integer.parseInt(value));
                } else {
                    customAttributes.put(key, value);
                }
            }
        }
        return customAttributes;
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
                NetworkClient.getInstance().removeListener(this);
                SceneManager.getInstance().switchScene("/view/fxml/DashboardView.fxml");
            } else {
                errorLabel.setStyle("-fx-text-fill: RED;");
                errorLabel.setText(response.message()); // Show server error message
            }
        });
    }
}