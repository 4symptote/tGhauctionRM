package com.app.shared.model.item;


public class Collectibles {
    // custom Vehicle fields
    private String brand;

    private Collectibles(Builder builder) {
        //super(builder.name, builder.desc, builder.startingPrice, builder.sellerId);
    }

    public static class Builder {
        private String name;
        private String desc;
        private double startingPrice;
        private String sellerId;

        private String brand;

        // base field setter
        public Builder name(String name) { this.name = name; return this; }
        public Builder desc(String desc) { this.desc = desc; return this; }
        public Builder startingPrice(double price) { this.startingPrice = price; return this; }
        public Builder sellerId(String id) { this.sellerId = id; return this; }
        //

        public Collectibles build() { return new Collectibles(this); }
    }
}