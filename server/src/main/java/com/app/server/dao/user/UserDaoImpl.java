package com.app.server.dao.user;

import com.app.server.dao.DatabaseConnection;
import com.app.shared.model.user.Admin;
import com.app.shared.model.user.Bidder;
import com.app.shared.model.user.Seller;
import com.app.shared.model.user.User;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class UserDaoImpl implements UserDao {
    private final MongoCollection<Document> usersCollection;

    public UserDaoImpl() {
        this.usersCollection = DatabaseConnection.getInstance().getDatabase().getCollection("users");
    }

    @Override
    public void saveUser(User user) {

        String role = "BIDDER";
        double balance = 0.0;

        if (user instanceof Admin) role = "ADMIN";
        else if (user instanceof Seller) role = "SELLER";
        else if (user instanceof Bidder b) {
            role = "BIDDER";
            balance = b.getBalance();
        }

        // object -> document
        Document doc = new Document("_id", user.getId())
                .append("username", user.getUsername())
                .append("passwordHash", user.getPassword())
                .append("email", user.getEmail())
                .append("role", role)
                .append("balance", balance);

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

    @Override
    public boolean userExists(String username) {
        return usersCollection.countDocuments(new Document("username", username)) > 0;
    }
}