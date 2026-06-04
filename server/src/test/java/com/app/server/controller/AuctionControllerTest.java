package com.app.server.controller;

import com.app.server.service.BidService;
import com.app.shared.exception.AuctionClosedException;
import com.app.shared.exception.AuctionNotFoundException;
import com.app.shared.exception.InvalidBidException;
import com.app.shared.model.auction.Auction;
import com.app.shared.model.item.Electronics;
import com.app.shared.model.user.Bidder;
import com.app.shared.network.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuctionControllerTest {

    @Test
    void placeBidReturnsPlacedBidResponse() {
        BidService bidService = mock(BidService.class);
        AuctionController controller = new AuctionController(bidService);
        Auction auction = runningAuction("auction-1", "seller-1", 100.0, 30);
        when(bidService.placeBid(eq("auction-1"), any(Bidder.class), eq(150.0))).thenReturn(auction);

        ResponseEntity<Response> response = controller.placeBid("auction-1", new AuctionController.PlaceBidRequest("bidder-1", "alice", 150.0));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().type()).isEqualTo(Response.ResponseType.PLACED_BID);
        var captor = forClass(Bidder.class);
        verify(bidService).placeBid(eq("auction-1"), captor.capture(), eq(150.0));
        assertThat(captor.getValue().getId()).isEqualTo("bidder-1");
        assertThat(captor.getValue().getUsername()).isEqualTo("alice");
    }

    @Test
    void exceptionHandlersMapDomainErrorsToHttpResponses() {
        AuctionController controller = new AuctionController(mock(BidService.class));

        Response notFound = controller.handleAuctionNotFound(new AuctionNotFoundException("missing")).getBody();
        Response badBid = controller.handleBadBid(new AuctionClosedException("closed")).getBody();
        Response invalidBid = controller.handleBadBid(new InvalidBidException("invalid")).getBody();

        assertThat(notFound.success()).isFalse();
        assertThat(notFound.message()).isEqualTo("missing");
        assertThat(badBid.success()).isFalse();
        assertThat(invalidBid.success()).isFalse();
    }

    private Auction runningAuction(String id, String sellerId, double startingPrice, long durationMinutes) {
        Electronics item = new Electronics.Builder()
                .name("Camera")
                .desc("Test camera")
                .startingPrice(startingPrice)
                .sellerId(sellerId)
                .brand("Sony")
                .build();
        long now = System.currentTimeMillis();
        Auction auction = new Auction(item, now - 1_000L, now + TimeUnit.MINUTES.toMillis(durationMinutes));
        auction.setId(id);
        auction.setSellerId(sellerId);
        auction.setSellerName("Seller");
        auction.setCurrentPrice(startingPrice);
        auction.updateStatus();
        return auction;
    }

}
