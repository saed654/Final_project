package com.example.final_projectsss.products;

public class Product {
    public String id;
    public String name;
    public double price;
    public int num;
    public String imgbase64;
    public boolean sala;

    public Product(){
        //empty constructor
    }
    public Product(String id, String name, double price, String image64) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imgbase64 = image64;
    }
    public Product(String id, String name, double price, String image64, boolean sala,int num) {
        this.num=num;
        this.sala = sala;
        this.id = id;
        this.name = name;
        this.price = price;
        this.imgbase64 = image64;
    }
}
