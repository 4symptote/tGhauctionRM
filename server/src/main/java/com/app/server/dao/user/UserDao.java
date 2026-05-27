package com.app.server.dao.user;

import com.app.shared.model.user.User;

public interface UserDao {
    void saveUser(User user);
    User getUserByUsername(String username);
    boolean userExists(String username);
}