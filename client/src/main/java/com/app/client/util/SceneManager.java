package com.app.client.util;

import com.app.client.controller.AuctionDetailController;
import com.app.shared.model.auction.Auction;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


// singleton ofcourse
public class SceneManager {
    private static final Logger logger = LoggerFactory.getLogger(SceneManager.class);
    private static SceneManager instance;
    private Stage primaryStage;

    private SceneManager() {}

    public static SceneManager getInstance() {
        if (instance == null) {
            instance = new SceneManager();
        }
        return instance;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public void switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            if (primaryStage.getScene() == null) {
                // First time launch
                Scene scene = new Scene(root);
                primaryStage.setScene(scene);
                primaryStage.setMinWidth(700);
                primaryStage.setMinHeight(700);
            } else {
                primaryStage.getScene().setRoot(root);
            }

            primaryStage.show();
        } catch (IOException e) {
            logger.error("Error: Failed to load FXML: {}", e.getMessage());
        }
    }

    // Add this new method below your existing switchScene()
    public void switchSceneWithData(String fxmlPath, Auction data) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof AuctionDetailController detailController) {
                detailController.initData(data);
            }

            if (primaryStage.getScene() == null) {
                primaryStage.setScene(new javafx.scene.Scene(root));
                primaryStage.setMinWidth(700);
                primaryStage.setMinHeight(700);
            } else {
                primaryStage.getScene().setRoot(root);
            }

            primaryStage.show();

        } catch (java.io.IOException e) {
            logger.error("Error loading FXML with data: {}", e.getMessage());
        }
    }
}