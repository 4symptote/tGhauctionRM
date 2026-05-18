package com.app.shared.model.user;

public class Bidder extends User {
    private double balance;

    @Override
    public boolean canBid() { return true; }

    public Bidder(String username, String password, String email, double initialBalance) {
        super(username, password, email, "BIDDER");
        this.balance = initialBalance;
    }


    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}

