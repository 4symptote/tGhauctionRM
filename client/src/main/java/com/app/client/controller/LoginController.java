package com.app.client.controller;

import com.app.client.model.SessionModel;
import com.app.client.network.NetworkClient;
import com.app.client.network.ResponseListener;

import com.app.client.util.SceneManager;

import com.app.shared.model.user.User;
import com.app.shared.network.Request;
import com.app.shared.network.Response;
import com.app.shared.network.payload.LoginPayload;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Observer:
public class LoginController implements ResponseListener {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    @FXML
    public void initialize() {
        // listen to = observe = subscribe the network client
        NetworkClient.getInstance().addListener(this);
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isBlank() || password.isBlank()) {
            errorLabel.setText("nhap ten va pw");
            return;
        }

        System.out.println("Attempting to login with: " + username);
        errorLabel.setText("connecting to server");

        LoginPayload payload = new LoginPayload(username, password);
        Request request = new Request(Request.RequestType.LOGIN, payload);
        NetworkClient.getInstance().sendRequest(request);
    }

    @FXML
    private void switchToRegister(ActionEvent event) {
        logger.info("switching to register");
        NetworkClient.getInstance().removeListener(this);
        SceneManager.getInstance().switchScene("/view/fxml/RegisterView.fxml");
    }


    @Override
    public void onResponseReceived(Response response) {
        // Response(success=false, ...) xem Response
        if (!response.success()) {
            errorLabel.setStyle("-fx-text-fill: red;");
            errorLabel.setText(response.message()); // message() -> "Sai ten, mk..."
            return; // return = exit
        }

        // success = true
        if (response.payload() instanceof User loggedInUser) {
            errorLabel.setStyle("-fx-text-fill: green;");
            errorLabel.setText("Welcome " + loggedInUser.getUsername());

            // Clean up? ko can observe nx
            // NetworkClient.getInstance().removeListener(this);

            SessionModel.getInstance().setCurrentUser(loggedInUser);

            // TODO: Switch to auction list (make da auction list view)
            SceneManager.getInstance().switchScene("/view/fxml/DashboardView.fxml");
            System.out.println("Switching to Dashboard...");
        }
    }
}