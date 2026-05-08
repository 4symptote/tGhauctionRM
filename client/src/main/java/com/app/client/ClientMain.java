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
    // Logger
    private static final Logger logger = LoggerFactory.getLogger(ClientMain.class);

    @Override
    public void start(Stage primaryStage) throws Exception {

        try { // Try to connect to server
            connect("localhost", 8080);
        } catch (Exception e) {
            logger.error("Error: Failed to connect to server: {}", e.getMessage());
        }

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


    public static void connect(String host, int port) throws Exception {
        // Tạo kết nối đến server
        Socket socket = new Socket(host, port);
        logger.info("Info: Connected to server successfully");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
