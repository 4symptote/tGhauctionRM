package com.app.shared.model.auction;

import com.app.shared.exception.AuctionClosedException;
import com.app.shared.exception.InvalidBidException;
import com.app.shared.model.item.Electronics;
import com.app.shared.model.item.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuctionModelTest {
    private Item item;

    @BeforeEach
    void setUp() {
        item = new Electronics.Builder()
                .name("Laptop")
                .desc("Gaming Laptop")
                .startingPrice(1000.0)
                .sellerId("seller-1")
                .brand("Dell")
                .build();
    }

    @Test
    void constructorShouldInitializeHappyPath() {
        long now = System.currentTimeMillis();
        Auction auction = new Auction(item, now - 1_000L, now + 60_000L);

        assertThat(auction.getId()).isNotBlank();
        assertThat(auction.getItem()).isSameAs(item);
        assertThat(auction.getCurrentPrice()).isEqualTo(1000.0);
        assertThat(auction.getStatus()).isEqualTo(Auction.Status.OPEN);
        assertThat(auction.getStartTime()).isEqualTo(now - 1_000L);
        assertThat(auction.getEndTimeMillis()).isEqualTo(now + 60_000L);
    }

    @Test
    void constructorShouldRejectNullItem() {
        long now = System.currentTimeMillis();

        assertThatThrownBy(() -> new Auction(null, now, now + 1_000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Item must not be null");
    }

    @Test
    void constructorShouldRejectInvalidTimeRange() {
        long now = System.currentTimeMillis();

        assertThatThrownBy(() -> new Auction(item, now + 1_000L, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("End time must be greater than start time");
    }

    @Test
    void updateStatusShouldMoveThroughOpenRunningAndFinishedAndRespectTerminalStates() {
        long now = System.currentTimeMillis();

        Auction openAuction = new Auction(item, now + 60_000L, now + 120_000L);
        openAuction.updateStatus();
        assertThat(openAuction.getStatus()).isEqualTo(Auction.Status.OPEN);

        Auction runningAuction = new Auction(item, now - 1_000L, now + 60_000L);
        runningAuction.updateStatus();
        assertThat(runningAuction.getStatus()).isEqualTo(Auction.Status.RUNNING);

        Auction finishedAuction = new Auction(item, now - 120_000L, now - 60_000L);
        finishedAuction.updateStatus();
        assertThat(finishedAuction.getStatus()).isEqualTo(Auction.Status.FINISHED);

        finishedAuction.setStatus(Auction.Status.PAID);
        finishedAuction.updateStatus();
        assertThat(finishedAuction.getStatus()).isEqualTo(Auction.Status.PAID);

        finishedAuction.setStatus(Auction.Status.CANCELED);
        finishedAuction.updateStatus();
        assertThat(finishedAuction.getStatus()).isEqualTo(Auction.Status.CANCELED);
    }

    @Test
    void processNewBidShouldUpdateStateAndRejectInvalidBids() {
        long now = System.currentTimeMillis();
        Auction auction = new Auction(item, now - 1_000L, now + 60_000L);
        auction.updateStatus();

        auction.processNewBid(1_250.0, "bidder-1", "Alice");

        assertThat(auction.getCurrentPrice()).isEqualTo(1_250.0);
        assertThat(auction.getHighestBidderId()).isEqualTo("bidder-1");
        assertThat(auction.getHighestBidderName()).isEqualTo("Alice");
        assertThat(item.getCurrentHighestBid()).isEqualTo(1_250.0);
        assertThat(auction.toString()).isEqualTo("Laptop | $1250.0 | RUNNING");

        assertThatThrownBy(() -> auction.processNewBid(1_000.0, "bidder-2", "Bob"))
                .isInstanceOf(InvalidBidException.class);

        Auction endedAuction = new Auction(item, now - 120_000L, now - 60_000L);
        assertThatThrownBy(() -> endedAuction.processNewBid(1_300.0, "bidder-3", "Carol"))
                .isInstanceOf(AuctionClosedException.class);
    }

    @Test
    void gettersAndSettersShouldWork() {
        long now = System.currentTimeMillis();
        Auction auction = new Auction(item, now - 1_000L, now + 60_000L);

        auction.setSellerId("seller-x");
        auction.setSellerName("Seller");
        auction.setCurrentPrice(1_100.0);
        auction.setStatus(Auction.Status.RUNNING);
        auction.setHighestBidderId("bidder-x");
        auction.setHighestBidderName("Bob");
        auction.setId("auction-x");

        assertThat(auction.getId()).isEqualTo("auction-x");
        assertThat(auction.getSellerId()).isEqualTo("seller-x");
        assertThat(auction.getSellerName()).isEqualTo("Seller");
        assertThat(auction.getCurrentPrice()).isEqualTo(1_100.0);
        assertThat(auction.getStatus()).isEqualTo(Auction.Status.RUNNING);
        assertThat(auction.getHighestBidderId()).isEqualTo("bidder-x");
        assertThat(auction.getHighestBidderName()).isEqualTo("Bob");
    }
}
