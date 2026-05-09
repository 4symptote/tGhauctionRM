package com.app.client;

public class Launcher {
    public static void main(String[] args) {
        System.setProperty("slf4j.internal.verbosity", "ERROR");
        System.setProperty("java.util.logging.config.file", "handlers=java.util.logging.ConsoleHandler .level=OFF java.util.logging.ConsoleHandler.level=OFF");
        // dont care abt those sh above

        ClientMain.main(args);
    }

    // Gọi main của ClientMain -> gọi launch() của Application Javafx -> Kết nối socket và hiển thị scene
}