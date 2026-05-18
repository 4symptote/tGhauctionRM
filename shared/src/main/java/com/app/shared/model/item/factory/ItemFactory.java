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
            "art", new ArtCreator(),
            "vehicle", new VehicleCreator(),
            "electronics", new ElectronicsCreator()
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

    // Factory method de tao item tu document
    public static Item createItemFromDocument(Document itemDoc) {
        String type = itemDoc.getString("type");
        String name = itemDoc.getString("name");
        String desc = itemDoc.getString("description");
        double price = itemDoc.getDouble("startingPrice");

        return switch (type) {
            case "Art" -> new Art.Builder()
                    .name(name).desc(desc).startingPrice(price)
                    .artist(itemDoc.getString("artist"))
                    .medium(itemDoc.getString("medium"))
                    .year(itemDoc.getInteger("year") != null ? itemDoc.getInteger("year") : 0)
                    .build();

            case "Vehicle" -> new Vehicle.Builder()
                    .name(name).desc(desc).startingPrice(price)
                    .brand(itemDoc.getString("brand"))
                    .model(itemDoc.getString("model"))
                    .build();

            case "Electronics" -> new Electronics.Builder()
                    .name(name).desc(desc).startingPrice(price)
                    .brand(itemDoc.getString("brand"))
                    .build();

            default -> throw new IllegalArgumentException("Unknown item type :" + type);
        };
    }



    private static String normalizeType(String itemType) {
        return itemType.trim().toLowerCase(Locale.ROOT);
    }
}
