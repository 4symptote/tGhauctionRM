package com.app.client.controller;

import com.app.client.network.NetworkClient;
import com.app.client.network.ResponseListener;
import com.app.client.util.ToastUtil;
import com.app.shared.network.Request;
import com.app.shared.network.Response;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class AdminDashboardController implements ResponseListener {

    @FXML private VBox mainContentVBox;
    @FXML private TextField auctionIdField;
    @FXML private TextField userIdField;


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
            double horizontalPadding = currentWidth * 0.1;

            horizontalPadding = Math.max(20, Math.min(100, horizontalPadding));
            mainContentVBox.setPadding(new javafx.geometry.Insets(30, horizontalPadding, 30, horizontalPadding));
        });
    }

    @FXML
    private void handleDeleteAuction() {
        String id = auctionIdField.getText().trim();
        if (id.isEmpty()) {
            ToastUtil.showToast("Please enter an Auction ID.", ToastUtil.ToastType.ERROR);
            return;
        }
        NetworkClient.getInstance().sendRequest(new Request(Request.RequestType.ADMIN_DELETE_AUCTION, id));
    }

    @FXML
    private void handleDeleteUser() {
        String id = userIdField.getText().trim();
        if (id.isEmpty()) {
            ToastUtil.showToast("Please enter a User ID.", ToastUtil.ToastType.ERROR);
            return;
        }
        NetworkClient.getInstance().sendRequest(new Request(Request.RequestType.ADMIN_DELETE_USER, id));
    }

    @Override
    public void onResponseReceived(Response response) {
        Platform.runLater(() -> {
            if (response.type() == Response.ResponseType.ADMIN_ACTION_SUCCESS) {
                if (response.success()) {
                    ToastUtil.showToast(response.message(), ToastUtil.ToastType.SUCCESS);
                    auctionIdField.clear();
                    userIdField.clear();
                } else {
                    ToastUtil.showToast(response.message(), ToastUtil.ToastType.ERROR);
                }
            }
        });
    }
}