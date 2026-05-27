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

    private static int activeToasts = 0;

    public static void showToast(String message, ToastType type) {
        Platform.runLater(() -> {

            Window activeWindow = Window.getWindows().stream()
                    .filter(Window::isFocused)
                    .findFirst()
                    .orElseGet(() -> Window.getWindows().isEmpty() ? null : Window.getWindows().get(0));

            if (activeWindow == null) return;

            Popup popup = new Popup();
            popup.setAutoFix(true);

            Label label = new Label(message);
            label.getStyleClass().add("toast-label");

            StackPane pane = new StackPane(label);
            pane.getStyleClass().addAll("toast-pane", "toast-" + type.name().toLowerCase());

            try {
                pane.getStylesheets().add(Objects.requireNonNull(
                        ToastUtil.class.getResource("/view/css/global.css")).toExternalForm());
            } catch (Exception ignored) {}

            popup.getContent().add(pane);
            popup.setOpacity(0);
            popup.show(activeWindow);

            double x = activeWindow.getX() + activeWindow.getWidth() - pane.getWidth() - 30;
            double y = activeWindow.getY() + 70 + (activeToasts * 60);
            popup.setX(x);
            popup.setY(y);

            activeToasts++; //

            Timeline timeline = new Timeline();
            KeyFrame fadeIn = new KeyFrame(Duration.millis(300), new KeyValue(popup.opacityProperty(), 1.0));
            KeyFrame fadeOutStart = new KeyFrame(Duration.millis(2800), new KeyValue(popup.opacityProperty(), 1.0));
            KeyFrame fadeOutEnd = new KeyFrame(Duration.millis(3300), new KeyValue(popup.opacityProperty(), 0.0));

            timeline.getKeyFrames().addAll(fadeIn, fadeOutStart, fadeOutEnd);
            timeline.setOnFinished(e -> {
                popup.hide();
                activeToasts--;
                if (activeToasts < 0) activeToasts = 0;
            });

            timeline.play();
        });
    }
}