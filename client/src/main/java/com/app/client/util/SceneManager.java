package com.app.client.util;

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
//            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
//            Parent root = loader.load();
//
//            Scene scene = new Scene(root);
//
//            boolean wasMaximized = primaryStage.isMaximized();
//
//            primaryStage.setScene(scene);
//            primaryStage.setMaximized(wasMaximized);
//            primaryStage.show();

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
}