package com.app.shared.model.user;

public class Bidder extends User {
    private double balance;
    private double reservedBalance;

    @Override
    public boolean canBid() { return true; }

    public Bidder(String username, String password, String email, double initialBalance, double reservedBalance) {
        super(username, password, email, "BIDDER");
        this.balance = initialBalance;
        this.reservedBalance = reservedBalance;
    }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public double getReservedBalance() { return reservedBalance; }
    public void setReservedBalance(double reservedBalance) { this.reservedBalance = reservedBalance; }
}
}

