package com.app.server.dao.user;

import com.app.shared.model.user.User;

public interface UserDao {
    void saveUser(User user);
    void updateUser(User user);
    User getUserByUsername(String username);
    User getUserById(String id);
    boolean userExists(String username);
}