package com.app.server.testutil;

import com.app.server.dao.DatabaseConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

public final class DatabaseConnectionMockHelper implements AutoCloseable {
    private final Field instanceField;
    private final DatabaseConnection previousInstance;

    public DatabaseConnectionMockHelper() {
        DatabaseConnection connection = mock(DatabaseConnection.class);
        MongoDatabase database = mock(MongoDatabase.class);
        MongoCollection<Document> collection = mock(MongoCollection.class);

        lenient().when(connection.getDatabase()).thenReturn(database);
        lenient().when(database.getCollection(anyString())).thenReturn(collection);

        try {
            instanceField = DatabaseConnection.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            previousInstance = (DatabaseConnection) instanceField.get(null);
            instanceField.set(null, connection);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to override DatabaseConnection singleton for tests", e);
        }
    }

    @Override
    public void close() {
        try {
            instanceField.set(null, previousInstance);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to restore DatabaseConnection singleton", e);
        }
    }
}
