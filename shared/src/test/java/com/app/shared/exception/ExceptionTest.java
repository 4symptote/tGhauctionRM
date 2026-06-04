package com.app.shared.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionTest {
    @Test
    void customExceptionsShouldCarryMessages() {
        AuctionClosedException auctionClosed = new AuctionClosedException("closed");
        AuctionNotFoundException notFound = new AuctionNotFoundException("missing");
        AuthenticationException auth = new AuthenticationException("auth");
        InvalidBidException invalidBid = new InvalidBidException("invalid");

        assertThat(auctionClosed).hasMessage("closed");
        assertThat(notFound).hasMessage("missing");
        assertThat(auth).hasMessage("auth");
        assertThat(invalidBid).hasMessage("invalid");
    }
}
