package com.app.client;

import com.app.client.network.NetworkClient;
import com.app.client.util.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientMain extends Application {

    private static final Logger logger = LoggerFactory.getLogger(ClientMain.class);
    private static final int port = 6767;

    @Override
    public void start(Stage primaryStage) {
        NetworkClient networkClient = NetworkClient.getInstance();
        SceneManager sceneManager = SceneManager.getInstance();

        networkClient.connect("localhost", port);
        networkClient.startListener();

        primaryStage.setTitle("tGhauctionRM");
        primaryStage.setMaximized(true);
        sceneManager.setPrimaryStage(primaryStage);
        sceneManager.switchScene("/view/fxml/LoginView.fxml");
    }

    public static void main(String[] args) {
        System.setProperty("slf4j.internal.verbosity", "ERROR");
        launch(args);
    }
}
