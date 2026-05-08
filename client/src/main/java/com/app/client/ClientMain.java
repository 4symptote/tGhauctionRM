package com.app.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;

public class ClientMain extends Application {

    private static final Logger logger = LoggerFactory.getLogger(ClientMain.class);
    private static final int port = 6767;

    @Override
    public void start(Stage primaryStage) {
        connect("localhost", port);

        // Setup simple scene for testing
        URL fxmlFile = getClass().getResource("/view/fxml/HellowScreen.fxml");
        assert fxmlFile != null;

        try {
            FXMLLoader loader = new FXMLLoader(fxmlFile);
            Parent root = loader.load();
            primaryStage.setScene(new Scene(root));
            primaryStage.show();
        } catch (IOException e) {
            logger.error("Error: Failed to load FXML: {}", e.getMessage());
        }
    }

    public static void connect(String host, int port) {
        try (Socket socket = new Socket(host, port)) {
            logger.info("Info: Connected to server successfully");
        } catch (Exception e) {
            logger.error("Error: Failed to connect to server: {}", e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.setProperty("slf4j.internal.verbosity", "ERROR");
        launch(args);
    }
}
