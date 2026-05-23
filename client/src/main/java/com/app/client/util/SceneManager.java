package com.app.client.util;

import com.app.client.controller.AuctionDetailController;
import com.app.shared.model.auction.Auction;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


// singleton ofcourse
public class SceneManager {
    private static final Logger logger = LoggerFactory.getLogger(SceneManager.class);
    private static SceneManager instance;
    private Stage primaryStage;

    private StackPane contentArea;

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

    public void setContentArea(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    public void switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            if (fxmlPath.contains("MainLayout") || fxmlPath.contains("Login") || fxmlPath.contains("Register")) {
                if (primaryStage.getScene() == null) {
                    Scene scene = new Scene(root);
                    primaryStage.setScene(scene);
                    primaryStage.setMinWidth(1000);
                    primaryStage.setMinHeight(700);
                } else {
                    primaryStage.getScene().setRoot(root);
                }
            } else {
                // subviews
                if (contentArea != null) {
                    contentArea.getChildren().clear();
                    contentArea.getChildren().add(root);
                }
            }

            primaryStage.show();
        } catch (IOException e) {
            logger.error("Error: Failed to load FXML: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    public void switchSceneWithData(String fxmlPath, Auction data) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof AuctionDetailController detailController) {
                detailController.initData(data);
            }

            if (fxmlPath.contains("MainLayout") || fxmlPath.contains("Login") || fxmlPath.contains("Register")) {
                if (primaryStage.getScene() == null) {
                    primaryStage.setScene(new Scene(root));
                    primaryStage.setMinWidth(1000);
                    primaryStage.setMinHeight(700);
                } else {
                    primaryStage.getScene().setRoot(root);
                }
            } else {
                if (contentArea != null) {
                    contentArea.getChildren().clear();
                    contentArea.getChildren().add(root);
                }
            }

            primaryStage.show();

        } catch (IOException e) {
            logger.error("Error loading FXML with data: {}", e.getMessage());
        }
    }

    public void logoutToLoginScreen() {
        this.contentArea = null; // Break the reference to the old shell
        switchScene("/view/fxml/LoginView.fxml");
    }
}