package com.app.shared.model.item.factory;

import com.app.shared.model.item.Art;
import com.app.shared.model.item.Electronics;
import com.app.shared.model.item.Item;
import com.app.shared.model.item.Vehicle;
import com.app.shared.network.payload.CreateAuctionPayload;
import org.bson.Document;

import java.util.Locale;
import java.util.Map;

public final class ItemFactory {
    private static final Map<String, ItemCreator> CREATORS = Map.of(
            "art"        , new ArtCreator(),
            "vehicle"    , new VehicleCreator(),
            "electronics", new ElectronicsCreator()
    );

    private ItemFactory() {}

    public static Item createItem(CreateAuctionPayload payload) {
        String normalizedType = normalizeType(payload.itemType());
        ItemCreator creator = CREATORS.get(normalizedType);
        if (creator == null) {
            throw new IllegalArgumentException("Unknown item type: " + payload.itemType());
        }
        return creator.createItem(payload);
    }

    // Factory method de tao item tu document
    public static Item createItemFromDocument(Document itemDoc) {
        String normalizedType = normalizeType(itemDoc.getString("type"));
        ItemCreator creator = CREATORS.get(normalizedType);
        if (creator == null) {
            throw new IllegalArgumentException("Unknown item type: " + itemDoc.getString("type"));
        }
        return creator.createItemFromDocument(itemDoc);
    }

    private static String normalizeType(String itemType) {
        return itemType.trim().toLowerCase(Locale.ROOT);
    }
}
