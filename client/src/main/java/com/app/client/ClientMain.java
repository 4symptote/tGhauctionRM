package com.app.client;

import com.app.client.network.NetworkClient;
import com.app.client.util.SceneManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

public class ClientMain extends Application {

    private static final Logger logger = LoggerFactory.getLogger(ClientMain.class);
    private static final int port = 6767;

    @Override
    public void start(Stage primaryStage) {
        NetworkClient.getInstance().connect("localhost", port);
        NetworkClient.getInstance().startListener();

        primaryStage.setTitle("tGhauctionRM");
        SceneManager.getInstance().setPrimaryStage(primaryStage);
        SceneManager.getInstance().switchScene("/view/fxml/LoginView.fxml");
    }

    public static void main(String[] args) {
        System.setProperty("slf4j.internal.verbosity", "ERROR");
        launch(args);
    }
}
