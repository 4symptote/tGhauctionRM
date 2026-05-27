package com.app.server.network;

import com.app.server.network.handler.*;
import com.app.shared.network.Request;
import com.app.shared.network.Response;

import java.util.EnumMap;
import java.util.Map;

public class RequestRouter {
    private final Map<Request.RequestType, RequestHandler> handlers = new EnumMap<>(Request.RequestType.class);

    public RequestRouter() {
        handlers.put(Request.RequestType.CREATE_AUCTION, new CreateAuctionHandler());
        handlers.put(Request.RequestType.PLACE_BID, new PlaceBidHandler());
        handlers.put(Request.RequestType.LOGIN, new LoginHandler());
        handlers.put(Request.RequestType.REGISTER, new RegisterHandler());
        handlers.put(Request.RequestType.GET_AUCTIONS, new GetAuctionsHandler());
        handlers.put(Request.RequestType.LOGOUT, new LogoutHandler());
        handlers.put(Request.RequestType.GET_BID_HISTORY, new GetBidHistoryHandler());
        handlers.put(Request.RequestType.GET_SELLER_AUCTIONS, new GetSellerAuctionsHandler());
        handlers.put(Request.RequestType.GET_WINNING_AUCTIONS, new GetWinningAuctionsHandler());
        handlers.put(Request.RequestType.DEPOSIT, new DepositHandler());
        handlers.put(Request.RequestType.WITHDRAW, new WithdrawHandler());
        handlers.put(Request.RequestType.SET_AUTO_BID, new SetAutoBidHandler());
    }

    public Response route(Request request, ClientHandler client) {

        RequestHandler handler = handlers.get(request.type());

        if (handler == null) {
            return new Response(false, "Unknown request type: " + request.type(), null);
        }
        return handler.handle(request, client);
    }
}
