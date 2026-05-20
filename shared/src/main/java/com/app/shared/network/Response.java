package com.app.shared.network;

import java.io.Serial;
import java.io.Serializable;

public record Response(ResponseType type, boolean success, String message, Object payload) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public enum ResponseType {
        GENERIC_SUCCESS,
        GENERIC_ERROR,
        AUCTION_LIST,      // For the Dashboard
        AUCTION_UPDATED,   // For live broadcasts
        BID_HISTORY,        // For the Detail View chart
        PLACED_BID,         // For successful bid
        BALANCE_UPDATED,     // For balance updates ( after bids, auction conclude, overtake,...)
        USER_UPDATED
    }

    // general success/error response
    public Response(boolean success, String message, Object payload) {
        this(success ? ResponseType.GENERIC_SUCCESS : ResponseType.GENERIC_ERROR, success, message, payload);
    }
}
