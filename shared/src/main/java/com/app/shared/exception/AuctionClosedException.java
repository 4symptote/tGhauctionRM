package com.app.shared.exception;

public class AuctionClosedException extends RuntimeException {
    public AuctionClosedException(String s) {
        super(s);
    }
}
