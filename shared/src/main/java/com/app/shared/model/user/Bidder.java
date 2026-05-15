package com.app.shared.model.user;

//import com.app.shared.model.auction.BidTransaction;
import java.util.ArrayList;
import java.util.List;

public class Bidder extends User {
    private double balance;

    public Bidder(String username, String password, String email, double initialBalance) {
        super(username, password, email, "BIDDER");
        this.balance = initialBalance;
    }


    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}

