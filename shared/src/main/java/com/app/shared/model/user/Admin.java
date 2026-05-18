package com.app.shared.model.user;

public class Admin extends User {
    @Override
    public boolean canBid() { return true; }
    @Override
    public boolean canSell() { return true; }
    @Override
    public boolean isAdmin() { return true; }

    public Admin(String username, String password, String email) {
        super(username, password, email, "ADMIN");
    }
}
