package com.app.shared.network.payload;

import java.io.Serial;
import java.io.Serializable;

public record AutoBidPayload(

        String auctionId,
        double maxLimit

) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}