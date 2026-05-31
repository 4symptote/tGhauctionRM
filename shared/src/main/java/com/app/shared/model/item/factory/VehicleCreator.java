package com.app.shared.model.item.factory;

import com.app.shared.model.item.Electronics;
import com.app.shared.model.item.Item;
import com.app.shared.model.item.Vehicle;
import com.app.shared.network.payload.CreateAuctionPayload;
import org.bson.Document;

import java.util.Map;

public class VehicleCreator implements ItemCreator {

    @Override
    public Item createItem(CreateAuctionPayload payload) {

        Map<String, Object> attrs = payload.customAttributes();
        if (attrs == null) attrs = Map.of();

        Item item = new Vehicle.Builder()
                .name(payload.name())
                .desc(payload.description())
                .startingPrice(payload.startingPrice())
                .model((String) attrs.getOrDefault("model", "Unknown"))
                .brand((String) attrs.getOrDefault("brand", "Unknown"))
                .build();
        item.setImageBase64(payload.imageBase64());
        return item;
    }

    @Override
    public Item createItemFromDocument(Document itemDoc) {
        Item item = new Vehicle.Builder()
                .name(itemDoc.getString("name"))
                .desc(itemDoc.getString("description"))
                .startingPrice(itemDoc.getDouble("startingPrice"))
                .brand(itemDoc.getString("brand"))
                .model(itemDoc.getString("model"))
                .build();
        item.setImageBase64(itemDoc.getString("imageBase64"));
        return item;
    }
}