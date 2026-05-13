package com.app.client.controller;

import com.app.client.network.NetworkClient;
import com.app.client.network.ResponseListener;
import com.app.client.util.SceneManager;
import com.app.shared.model.user.User;
import com.app.shared.network.Request;
import com.app.shared.network.Response;
import com.app.shared.network.payload.RegisterPayload;
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

public class RegisterController implements ResponseListener {
    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label errorLabel;
    @FXML private Button registerButton;

    @FXML
    public void initialize() {
        NetworkClient.getInstance().addListener(this);
    }

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

        System.out.println("Attempting to register: " + username + " as " + role);
        errorLabel.setStyle("-fx-text-fill: green;");
        errorLabel.setText("Sending registration...");

        RegisterPayload payload = new RegisterPayload(username, password, email, role);
        Request request = new Request(Request.RequestType.REGISTER, payload);
        NetworkClient.getInstance().sendRequest(request);
    }

    @FXML
    private void switchToLogin(ActionEvent event) {
        logger.info("switching to login");
        SceneManager.getInstance().switchScene("/view/fxml/LoginView.fxml");
    }

    @Override
    public void onResponseReceived(Response response) {
        if (!response.success()) {
            errorLabel.setStyle("-fx-text-fill: red;");
            errorLabel.setText(response.message());
            return;
        }

        //
        if (response.payload() instanceof User newUser) {
            errorLabel.setStyle("-fx-text-fill: green;");
            errorLabel.setText("Account created! Logging you in...");

            // Clean up?
            // NetworkClient.getInstance().removeListener(this);


            System.out.println("Switching to Dashboard...");
            SceneManager.getInstance().switchScene("/view/fxml/HellowScreen.fxml");
        }
    }
}