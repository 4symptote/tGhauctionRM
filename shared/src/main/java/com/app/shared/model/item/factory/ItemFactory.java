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
        ItemCreator creator = getCreator(payload.itemType());
        return creator.createItem(payload);
    }

    // Factory method de tao item tu document
    public static Item createItemFromDocument(Document itemDoc) {
        ItemCreator creator = getCreator(itemDoc.getString("type"));
        return creator.createItemFromDocument(itemDoc);
    }

    private static ItemCreator getCreator(String itemType) {
        ItemCreator creator = CREATORS.get(normalizeType(itemType));
        if (creator == null) throw new IllegalArgumentException("Unknown item type: " + itemType);
        return creator;
    }

    private static String normalizeType(String itemType) {
        return itemType.trim().toLowerCase(Locale.ROOT);
    }
}
