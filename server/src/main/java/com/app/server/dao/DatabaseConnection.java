package com.app.server.dao;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);

    private static DatabaseConnection instance;
    /**
     *
     */
    private MongoClient mongoClient;
    private MongoDatabase database;

    private DatabaseConnection() {
        try {
            Dotenv dotenv = Dotenv.configure().directory("./server").load();
            String CONNECTION_STRING = dotenv.get("MONGO_URI");
            String DATABASE_NAME = dotenv.get("DATABASE_NAME");

            if (CONNECTION_STRING == null || DATABASE_NAME == null) {
                throw new RuntimeException("MONGO_URI or DATABASE_NAME is not set in .env file or .env file is missing");
            }

            logger.info("Connecting to MongoDB Atlas...");
            mongoClient = MongoClients.create(CONNECTION_STRING);
            database = mongoClient.getDatabase(DATABASE_NAME);
            logger.info("Successfully connected to database: {}", DATABASE_NAME);
        } catch (Exception e) {
            logger.error("Failed to connect to MongoDB: {}", e.getMessage());
            System.exit(1); // Crash the server if the DB is down
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            logger.info("MongoDB connection closed");
        }
    }
}