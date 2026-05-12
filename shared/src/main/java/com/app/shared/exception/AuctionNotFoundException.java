package com.app.shared.exception;

public class AuctionNotFoundException extends RuntimeException {
    public AuctionNotFoundException(String s) {
        super(s);
    }
}
