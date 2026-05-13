package com.app.client.controller;

import com.app.client.network.NetworkClient;
import com.app.shared.network.Request;
import com.app.shared.network.Response;
import com.app.shared.network.payload.CreateAuctionPayload;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Text;

public class TestController {
    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @FXML private TextField nameField;
    @FXML private TextArea descField;
    @FXML private TextField durationField;
    @FXML private TextField startingPriceField;


    @FXML private Button createAuctionButton;
    @FXML private ComboBox<String> typeComboBox;


    @FXML
    public void initialize() {
        logger.info("TestController initialized");
    }

    @FXML
    private void handleCreateAuction() {
        String desc = descField.getText();
        long duration = Long.parseLong(durationField.getText());
        double startingPrice = Double.parseDouble(startingPriceField.getText());
        String type = typeComboBox.getValue();
        String name = nameField.getText();

        CreateAuctionPayload payload = new CreateAuctionPayload(type, name, desc, startingPrice, "", duration, null);
        Request request = new Request(Request.RequestType.CREATE_AUCTION, payload);
        NetworkClient.getInstance().sendRequest(request);
    }

}
