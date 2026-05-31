package com.app.shared.network.payload;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

public record CreateAuctionPayload (

        String itemType,
        String name,
        String description,
        double startingPrice,
        long startTime,   // milliseconds
        long durationMillis,
        Map<String, Object> customAttributes, // For extra attributes
        String imageBase64

) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
