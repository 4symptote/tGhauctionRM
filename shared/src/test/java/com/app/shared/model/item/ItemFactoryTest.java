package com.app.shared.model.item;

import com.app.shared.model.item.factory.ItemFactory;
import com.app.shared.network.payload.CreateAuctionPayload;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemFactoryTest {
    @Test
    void electronicsBuilderAndFactoryShouldCreateExpectedItem() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("brand", "Dell");

        CreateAuctionPayload payload = new CreateAuctionPayload(
                "Electronics",
                "Laptop",
                "Gaming Laptop",
                1000.0,
                1L,
                10L,
                attributes,
                "base64"
        );

        Item item = ItemFactory.createItem(payload);
        assertThat(item).isInstanceOf(Electronics.class);
        assertThat(item.getName()).isEqualTo("Laptop");
        assertThat(item.getDescription()).isEqualTo("Gaming Laptop");
        assertThat(item.getStartingPrice()).isEqualTo(1000.0);
        assertThat(item.getImageBase64()).isEqualTo("base64");
        assertThat(((Electronics) item).getBrand()).isEqualTo("Dell");

        Document document = item.toBsonDocument();
        Item fromDocument = ItemFactory.createItemFromDocument(document);
        assertThat(fromDocument).isInstanceOf(Electronics.class);
        assertThat(fromDocument.getName()).isEqualTo("Laptop");
        assertThat(((Electronics) fromDocument).getBrand()).isEqualTo("Dell");
    }

    @Test
    void artAndVehicleCreatorsShouldUseDefaultsWhenAttributesMissing() {
        CreateAuctionPayload artPayload = new CreateAuctionPayload(
                "Art",
                "Painting",
                "Oil on canvas",
                250.0,
                1L,
                10L,
                null,
                null
        );
        Item art = ItemFactory.createItem(artPayload);
        assertThat(art).isInstanceOf(Art.class);
        assertThat(((Art) art).getArtist()).isEqualTo("Unknown");
        assertThat(((Art) art).getYear()).isEqualTo(0);

        CreateAuctionPayload vehiclePayload = new CreateAuctionPayload(
                "Vehicle",
                "Car",
                "Sedan",
                5000.0,
                1L,
                10L,
                Map.of(),
                null
        );
        Item vehicle = ItemFactory.createItem(vehiclePayload);
        assertThat(vehicle).isInstanceOf(Vehicle.class);
        assertThat(((Vehicle) vehicle).getBrand()).isEqualTo("Unknown");
        assertThat(((Vehicle) vehicle).getModel()).isEqualTo("Unknown");
    }

    @Test
    void factoryShouldRejectUnknownType() {
        CreateAuctionPayload payload = new CreateAuctionPayload(
                "Unknown",
                "Thing",
                "Desc",
                1.0,
                1L,
                10L,
                Map.of(),
                null
        );

        assertThatThrownBy(() -> ItemFactory.createItem(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown item type");
    }
}
