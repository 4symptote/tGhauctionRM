package com.app.client.util;

import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Objects;

public class ImageUtil {

    private static final Logger log = LoggerFactory.getLogger(ImageUtil.class);

    // chuyen image thanh base64 string
    public static String encodeToBase64(File file) {
        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (Exception e) {
            log.error("Error encoding file to base64: {}", e.getMessage());
            return null;
        }
    }

    // base64 -> image
    public static Image decodeToImage(String base64) {
        if (base64 == null || base64.trim().isEmpty()) {
            // Return a default placeholder if the item has no image
            return new Image(Objects.requireNonNull(ImageUtil.class.getResourceAsStream("/images/placeholder.png")));
        }
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64);
            return new Image(new ByteArrayInputStream(imageBytes));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}