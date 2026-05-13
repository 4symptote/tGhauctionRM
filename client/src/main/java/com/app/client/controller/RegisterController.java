package com.app.client.controller;

import com.app.client.util.SceneManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// import com.app.client.network.NetworkClient;
// import com.app.shared.network.payload.RegisterPayload;

public class RegisterController {
    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label errorLabel;
    @FXML private Button registerButton;

    @FXML
    private void handleRegister(ActionEvent event) {
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();

        if (username.isBlank() || email.isBlank() || password.isBlank() || role == null) {
            errorLabel.setText("Please fill out all fields.");
            return;
        }

        // TODO: Send RegisterPayload to server
        // NetworkClient.getInstance().sendRegisterRequest(new RegisterPayload(username, password, email, role));

        System.out.println("Attempting to register: " + username + " as " + role);
        errorLabel.setStyle("-fx-text-fill: green;");
        errorLabel.setText("Sending registration...");

        SceneManager.getInstance().switchScene("/view/fxml/HellowScreen.fxml");
    }

    @FXML
    private void switchToLogin(ActionEvent event) {
        logger.info("switching to login");
        SceneManager.getInstance().switchScene("/view/fxml/LoginView.fxml");
    }
}