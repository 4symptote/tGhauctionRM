package com.app.shared.network;

import com.app.shared.network.payload.AutoBidPayload;
import com.app.shared.network.payload.BidPayload;
import com.app.shared.network.payload.CreateAuctionPayload;
import com.app.shared.network.payload.LoginPayload;
import com.app.shared.network.payload.RegisterPayload;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NetworkMessageTest {
    @Test
    void requestAndResponseRecordsShouldExposeFields() {
        Request request = new Request(Request.RequestType.PLACE_BID, new BidPayload("auction-1", 123.0));
        Response success = new Response(true, "ok", Map.of("a", 1));
        Response error = new Response(false, "bad", null);

        assertThat(request.type()).isEqualTo(Request.RequestType.PLACE_BID);
        assertThat(request.payload()).isInstanceOf(BidPayload.class);
        assertThat(success.type()).isEqualTo(Response.ResponseType.GENERIC_SUCCESS);
        assertThat(success.success()).isTrue();
        assertThat(success.message()).isEqualTo("ok");
        assertThat(error.type()).isEqualTo(Response.ResponseType.GENERIC_ERROR);
        assertThat(error.success()).isFalse();
    }

    @Test
    void payloadRecordsShouldExposeValues() {
        LoginPayload login = new LoginPayload("alice", "secret");
        RegisterPayload register = new RegisterPayload("bob", "secret123", "bob@example.com", "BIDDER");
        AutoBidPayload autoBid = new AutoBidPayload("auction-1", 500.0);
        CreateAuctionPayload create = new CreateAuctionPayload("Electronics", "Laptop", "Desc", 1000.0, 1L, 10L, Map.of(), null);

        assertThat(login.username()).isEqualTo("alice");
        assertThat(login.password()).isEqualTo("secret");
        assertThat(register.role()).isEqualTo("BIDDER");
        assertThat(autoBid.auctionId()).isEqualTo("auction-1");
        assertThat(autoBid.maxLimit()).isEqualTo(500.0);
        assertThat(create.itemType()).isEqualTo("Electronics");
        assertThat(create.startingPrice()).isEqualTo(1000.0);
    }
}
