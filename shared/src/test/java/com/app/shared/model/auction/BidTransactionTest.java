package com.app.shared.model.auction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BidTransactionTest {
    @Test
    void compactConstructorShouldPopulateGeneratedIdAndTimestamp() {
        BidTransaction bid = new BidTransaction("auction-1", "user-1", "Alice", 123.45);

        assertThat(bid.id()).isNotBlank();
        assertThat(bid.auctionId()).isEqualTo("auction-1");
        assertThat(bid.bidderId()).isEqualTo("user-1");
        assertThat(bid.bidderName()).isEqualTo("Alice");
        assertThat(bid.amount()).isEqualTo(123.45);
        assertThat(bid.timestamp()).isPositive();
    }

    @Test
    void recordEqualityShouldUseAllComponents() {
        BidTransaction one = new BidTransaction("id-1", "auction-1", "user-1", "Alice", 123.45, 10L);
        BidTransaction two = new BidTransaction("id-1", "auction-1", "user-1", "Alice", 123.45, 10L);
        BidTransaction three = new BidTransaction("id-2", "auction-1", "user-1", "Alice", 123.45, 10L);

        assertThat(one).isEqualTo(two);
        assertThat(one).hasSameHashCodeAs(two);
        assertThat(one).isNotEqualTo(three);
    }
}
