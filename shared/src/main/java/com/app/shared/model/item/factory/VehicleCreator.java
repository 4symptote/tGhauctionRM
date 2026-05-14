package com.app.shared.model.item.factory;

import com.app.shared.model.item.Electronics;
import com.app.shared.model.item.Item;
import com.app.shared.model.item.Vehicle;
import com.app.shared.network.payload.CreateAuctionPayload;

import java.util.Map;

public class VehicleCreator implements ItemCreator {

    @Override
    public Item createItem(CreateAuctionPayload payload) {

        Map<String, Object> attrs = payload.customAttributes();
        if (attrs == null) {
            attrs = Map.of();
        }

        return new Vehicle.Builder()
                .name(payload.name())
                .desc(payload.description())
                .sellerId(payload.sellerId())
                .startingPrice(payload.startingPrice())

                .brand((String) attrs.getOrDefault("brand", "Unknown"))

                .build();

    }
}