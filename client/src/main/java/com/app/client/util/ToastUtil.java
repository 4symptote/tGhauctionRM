package com.app.client.util;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.Objects;

public class ToastUtil {

    public enum ToastType {
        SUCCESS, ERROR, INFO
    }

    public static void showToast(String message, ToastType type) {
        Platform.runLater(() -> {
            System.out.println(message);
            Window activeWindow = Window.getWindows().stream()
                    .filter(Window::isFocused)
                    .findFirst()
                    .orElse(null);

            if (activeWindow == null) return;

            Popup popup = new Popup();
            popup.setAutoFix(true);

            // Build the UI
            Label label = new Label(message);
            label.getStyleClass().add("toast-label");

            StackPane pane = new StackPane(label);
            pane.getStyleClass().addAll("toast-pane", "toast-" + type.name().toLowerCase());

            // apply css
            try {
                pane.getStylesheets().add(Objects.requireNonNull(
                        ToastUtil.class.getResource("/view/css/global.css")).toExternalForm());
            } catch (Exception e) {
                System.out.println("Could not load global.css for Toast");
            }

            popup.getContent().add(pane);

            // Temporarily show it invisibly to calculate its width/height
            popup.setOpacity(0);
            popup.show(activeWindow);

            // Position it at the Top-Right of the active window
            double x = activeWindow.getX() + activeWindow.getWidth() - pane.getWidth() - 30;
            double y = activeWindow.getY() + 70;
            popup.setX(x);
            popup.setY(y);

            // Animation: Fade in (0.3s) -> Wait (2.5s) -> Fade out (0.5s) -> Close
            Timeline timeline = new Timeline();

            // Fade in
            KeyFrame fadeIn = new KeyFrame(Duration.millis(300), new KeyValue(popup.opacityProperty(), 1.0));
            // Start fading out after 2.5 seconds
            KeyFrame fadeOutStart = new KeyFrame(Duration.millis(2800), new KeyValue(popup.opacityProperty(), 1.0));
            // Fully transparent and close at 3.3 seconds
            KeyFrame fadeOutEnd = new KeyFrame(Duration.millis(3300), new KeyValue(popup.opacityProperty(), 0.0));

            timeline.getKeyFrames().addAll(fadeIn, fadeOutStart, fadeOutEnd);
            timeline.setOnFinished(e -> popup.hide());

            timeline.play();
        });
    }
}