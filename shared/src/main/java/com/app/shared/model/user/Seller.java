package com.app.shared.model.user;

public class Seller extends User {
    private double totalRevenue;

    @Override
    public boolean canSell() { return true; }

    public Seller(String username, String password, String email, double initialBalance) {
        super(username, password, email, "SELLER");
        this.totalRevenue = initialBalance;
    }

    public void collectRevenue(double amount) {
        this.totalRevenue += amount;
    }

    public double getTotalRevenue() { return totalRevenue; }
}
