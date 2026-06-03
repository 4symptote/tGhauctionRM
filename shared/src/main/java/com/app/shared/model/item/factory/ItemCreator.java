package com.app.shared.model.item.factory;

import com.app.shared.model.item.Item;
import com.app.shared.network.payload.CreateAuctionPayload;

public interface ItemCreator {
    Item createItem(CreateAuctionPayload payload);
    Item createItemFromDocument(org.bson.Document document);
}