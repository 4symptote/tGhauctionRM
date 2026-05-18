package com.app.shared.model.item;

import org.bson.Document;

public class Art extends Item {
    // Custom Art Fields
    private final String artist;
    private final String medium;
    private final int year;

    // chỉ tạo được Art item qua Builder. no 'new'
    private Art(Builder builder) {
        super(builder.name, builder.desc, builder.startingPrice, builder.sellerId);
        this.artist = builder.artist;
        this.medium = builder.medium;
        this.year = builder.year;
    }

    // Da Builder. xem ArtCreator
    public static class Builder {
        // Base fields
        private String name;
        private String desc;
        private double startingPrice;
        private String sellerId;
        // Custom attributes
        private String artist;
        private String medium;
        private int year;

        // base field setter
        public Builder name(String name) { this.name = name; return this; }
        public Builder desc(String desc) { this.desc = desc; return this; }
        public Builder startingPrice(double price) { this.startingPrice = price; return this; }
        public Builder sellerId(String id) { this.sellerId = id; return this; }
        // custom attributes setter
        public Builder artist(String artist) { this.artist = artist; return this; }
        public Builder medium(String medium) { this.medium = medium; return this; }
        public Builder year(int year) { this.year = year; return this; }

        // final build
        public Art build() {
            return new Art(this);
        }
    }

    @Override
    public Document toBsonDocument() {
        return new Document("type", "Art")
                .append("name", getName())
                .append("description", getDescription())
                .append("startingPrice", getStartingPrice())
                .append("artist", this.artist)
                .append("medium", this.medium)
                .append("year", this.year);
    }

    public int getYear() {
        return year;
    }

    public String getArtist() {
        return artist;
    }

    public String getMedium() {
        return medium;
    }
}

// Example builder:
/* Art art = new Art.Builder()
            .name("Painting")
            .desc("Painting of a cat")
            .startingPrice(100.0)
            .sellerId("123")
            .artist("John")
            .medium("Oil")
            .year(2020)
            .build(); // final build
 */
