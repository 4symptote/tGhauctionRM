package com.app.shared.model.item.factory;

import com.app.shared.model.item.Item;
import com.app.shared.network.payload.CreateAuctionPayload;

import java.util.Locale;
import java.util.Map;

public final class ItemFactory {
    private static final Map<String, ItemCreator> CREATORS = Map.of(
            "art", new ArtCreator()
    );

    private ItemFactory() {}

    public static Item createItem(CreateAuctionPayload payload) {
        String normalizedType = normalizeType(payload.itemType());
        ItemCreator creator = CREATORS.get(normalizedType);
        if (creator == null) {
            throw new IllegalArgumentException("Unsupported item type: " + payload.itemType());
        }

        return creator.createItem(payload);
    }

    private static String normalizeType(String itemType) {
        if (itemType == null || itemType.isBlank()) {
            return "electronics";
        }
        return itemType.trim().toLowerCase(Locale.ROOT);
    }
}
