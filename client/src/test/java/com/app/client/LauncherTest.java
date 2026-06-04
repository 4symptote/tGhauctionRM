package com.app.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class LauncherTest {

    @AfterEach
    void tearDown() {
        System.clearProperty("slf4j.internal.verbosity");
        System.clearProperty("java.util.logging.config.file");
    }

    @Test
    void mainDelegatesToClientMainAndSetsLoggingProperties() {
        try (MockedStatic<ClientMain> mocked = mockStatic(ClientMain.class)) {
            String[] args = {"alpha", "beta"};

            Launcher.main(args);

            assertThat(System.getProperty("slf4j.internal.verbosity")).isEqualTo("ERROR");
            assertThat(System.getProperty("java.util.logging.config.file"))
                    .contains("ConsoleHandler.level=OFF");
            mocked.verify(() -> ClientMain.main(args));
        }
    }
}
