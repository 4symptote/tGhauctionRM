package com.app.client.util;

import com.app.client.testutil.TestSupport;
import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class ImageUtilTest {

    @BeforeAll
    static void startFx() {
        TestSupport.initJavaFx();
    }

    @Test
    void encodeToBase64ReadsFileContents() throws Exception {
        File temp = File.createTempFile("image-util", ".bin");
        temp.deleteOnExit();
        Files.writeString(temp.toPath(), "hello", StandardCharsets.UTF_8);

        String encoded = ImageUtil.encodeToBase64(temp);

        assertThat(encoded).isEqualTo(Base64.getEncoder().encodeToString("hello".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void decodeToImageReturnsNullForInvalidBase64() {
        assertThat(ImageUtil.decodeToImage("not-base64")).isNull();
    }

    @Test
    void decodeToImageReturnsPlaceholderForBlankInput() {
        Image image = ImageUtil.decodeToImage("   ");

        assertThat(image).isNotNull();
    }
}
