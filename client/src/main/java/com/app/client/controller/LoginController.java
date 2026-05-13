package com.app.client.controller;

import com.app.client.util.SceneManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// import com.app.client.network.NetworkClient;
// import com.app.shared.network.payload.LoginPayload;

public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

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
        logger.info("switching to register");
        SceneManager.getInstance().switchScene("/view/fxml/RegisterView.fxml");
    }
}