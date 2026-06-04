package com.app.server.controller;

import com.app.server.service.BidService;
import com.app.shared.exception.AuctionClosedException;
import com.app.shared.exception.AuctionNotFoundException;
import com.app.shared.exception.InvalidBidException;
import com.app.shared.model.auction.Auction;
import com.app.shared.model.user.Bidder;
import com.app.shared.network.Response;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.LinkedHashMap;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/auctions")
public class AuctionController {
    private final BidService bidService;

    public AuctionController(BidService bidService) {
        this.bidService = bidService;
    }

    @PostMapping("/{id}/bids")
    public ResponseEntity<Response> placeBid(@PathVariable("id") String auctionId,
                                             @Valid @RequestBody PlaceBidRequest request) {
        Bidder bidder = new Bidder(request.bidderName(), "", "", 0.0, 0.0);
        bidder.setId(request.bidderId());

        Auction updatedAuction = bidService.placeBid(auctionId, bidder, request.bidAmount());
        return ResponseEntity.ok(new Response(Response.ResponseType.PLACED_BID, true, "Bid placed successfully", updatedAuction));
    }

    @ExceptionHandler(AuctionNotFoundException.class)
    public ResponseEntity<Response> handleAuctionNotFound(AuctionNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new Response(false, exception.getMessage(), null));
    }

    @ExceptionHandler({AuctionClosedException.class, InvalidBidException.class})
    public ResponseEntity<Response> handleBadBid(RuntimeException exception) {
        return ResponseEntity.badRequest()
                .body(new Response(false, exception.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldError() != null ? exception.getBindingResult().getFieldError().getDefaultMessage()
                : "Invalid request body";
        return ResponseEntity.badRequest()
                .body(new Response(false, message, null));
    }

    public record PlaceBidRequest(
            @NotBlank(message = "bidderId is required")
            String bidderId,
            @NotBlank(message = "bidderName is required")
            String bidderName,
            @NotNull(message = "bidAmount is required")
            @Positive(message = "bidAmount must be positive")
            Double bidAmount
    ) {
    }
}
