package com.example.final_projectsss.products;

public class OrderModel {
    public String id;
    public String userId;
    public String userEmail;
    public String productId;
    public String productName;
    public String image;
    public double price;
    public int quantity;

    public OrderModel() {
    }

    public OrderModel(String id, String userId, String userEmail,
                      String productId, String productName, String image,
                      double price, int quantity) {
        this.id = id;
        this.userId = userId;
        this.userEmail = userEmail;
        this.productId = productId;
        this.productName = productName;
        this.image = image;
        this.price = price;
        this.quantity = quantity;
    }
}