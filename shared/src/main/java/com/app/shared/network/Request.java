package com.app.shared.network;

import java.io.Serial;
import java.io.Serializable;

public record Request(RequestType type, Object payload) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public enum RequestType {
        LOGIN,
        LOGOUT,
        REGISTER,
        PLACE_BID,
        CREATE_AUCTION,
        DELETE_AUCTION,
        SET_AUCTION_PRICE,
        CONCLUDE_AUCTION,
        GET_AUCTIONS,
        GET_BID_HISTORY,
        GET_SELLER_AUCTIONS, // --> SELLER_AUCTION_LIST
        GET_WINNING_AUCTIONS,
        WITHDRAW,
        DEPOSIT,
    }
}
