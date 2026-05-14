package com.app.shared.model.item;


public class Vehicle extends Item {
    // custom Vehicle fields
    private String brand;
    private String model;

    private Vehicle(Builder builder) {
        super(builder.name, builder.desc, builder.startingPrice, builder.sellerId);
        this.brand = builder.brand;
        this.model = builder.model;
    }

    public static class Builder {
        private String name;
        private String desc;
        private double startingPrice;
        private String sellerId;

        private String brand;
        private String model;

        // base field setter
        public Builder name(String name) { this.name = name; return this; }
        public Builder desc(String desc) { this.desc = desc; return this; }
        public Builder startingPrice(double price) { this.startingPrice = price; return this; }
        public Builder sellerId(String id) { this.sellerId = id; return this; }
        //
        public Builder brand(String brand) { this.brand = brand; return this; }
        public Builder model(String model) { this.model = model; return this; }

        public Vehicle build() { return new Vehicle(this); }

    }

    public String getBrand() { return this.brand; }
    public String getModel() { return this.model; }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public void setModel(String model) {
        this.model = model;
    }
}