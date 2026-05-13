package com.app.shared.model.item;


public class Electronics extends Item {
    // custom Vehicle fields
    private String brand;

    private Electronics(Builder builder) {
        super(builder.name, builder.desc, builder.startingPrice, builder.sellerId);
        this.brand = builder.brand;
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
        public Builder brand(String brand) { this.brand = brand; return this; }

        public Electronics build() { return new Electronics(this); }

    }

    public String getBrand() { return this.brand; }

    public Electronics setBrand(String brand) {
        this.brand = brand;
        return this;
    }
}