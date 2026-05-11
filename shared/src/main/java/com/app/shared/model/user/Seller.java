package com.app.shared.model.user;


public class Seller extends User {
    private double totalRevenue;

    public Seller(String username, String password, String email) {
        super(username, password, email, "SELLER");
        this.totalRevenue = 0.0;
    }

    public void collectRevenue(double amount) {
        this.totalRevenue += amount;
    }

    public double getTotalRevenue() { return totalRevenue; }
}
