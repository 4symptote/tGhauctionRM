package com.app.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
// import com.app.client.network.NetworkClient;
// import com.app.shared.network.payload.LoginPayload;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isBlank() || password.isBlank()) {
            errorLabel.setText("nhap ten va pw");
            return;
        }

        // todo: Send LoginPayload to server using NetworkClient
        // NetworkClient.getInstance().sendLoginRequest(new LoginPayload(username, password));

        System.out.println("Attempting to login with: " + username);
        errorLabel.setText("connecting to server");
    }

    @FXML
    private void switchToRegister(ActionEvent event) {
        System.out.println("Switching to Register Scene...");
        // TODO: Use FXMLLoader to load RegisterView.fxml
    }
}