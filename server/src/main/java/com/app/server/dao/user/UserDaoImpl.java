package com.app.server.dao.user;

import com.app.server.dao.DatabaseConnection;
import com.app.shared.model.user.Admin;
import com.app.shared.model.user.Bidder;
import com.app.shared.model.user.Seller;
import com.app.shared.model.user.User;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class UserDaoImpl implements UserDao {
    private static final UserDaoImpl instance = new UserDaoImpl();

    private final MongoCollection<Document> usersCollection;

    private UserDaoImpl() {
        this.usersCollection = DatabaseConnection.getInstance().getDatabase().getCollection("users");
    }

    public static UserDaoImpl getInstance() {
        return instance;
    }

    @Override
    public void saveUser(User user) {
        Document doc = userToDocument(user);
        usersCollection.insertOne(doc);
    }

    @Override
    public User getUserByUsername(String username) {
        Document query = new Document("username", username);
        Document doc = usersCollection.find(query).first();

        if (doc == null) {
            return null;
        }
        // MongoDB Document -> Java Object
        return documentToUser(doc);
    }

    @Override
    public User getUserById(String id) {
        Document query = new Document("_id", id);
        Document doc = usersCollection.find(query).first();
        if (doc == null) {
            return null;
        }

        return documentToUser(doc);
    }

    @Override
    public void updateUser(User user) {
        Document doc = userToDocument(user);
        usersCollection.replaceOne(new Document("_id", user.getId()), doc);
    }

    private User documentToUser(Document doc) {
        String dbId = doc.getString("_id");
        String fetchedUsername = doc.getString("username");
        String passwordHash = doc.getString("passwordHash");
        String email = doc.getString("email");
        String role = doc.getString("role");

        User user = switch (role) {
            case "ADMIN" -> new Admin(fetchedUsername, passwordHash, email);
            case "SELLER" -> new Seller(fetchedUsername, passwordHash, email);
            default -> {
                double balance = doc.getDouble("balance");
                yield new Bidder(fetchedUsername, passwordHash, email, balance);
            }
        };
        user.setId(dbId);
        return user;
    }

    private Document userToDocument(User user) {
        double balance = 0.0;
        if (user instanceof Bidder b) balance = b.getBalance();
        if (user instanceof Seller s) balance = s.getTotalRevenue();
        return new Document("_id", user.getId())
                .append("username", user.getUsername())
                .append("passwordHash", user.getPassword())
                .append("email", user.getEmail())
                .append("role", user.getRole())
                .append("balance", balance);
    }

    @Override
    public boolean userExists(String username) {
        return usersCollection.countDocuments(new Document("username", username)) > 0;
    }
}