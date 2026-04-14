package com.example.final_projectsss.molds;

public class SlotModel {

    public String id;
    public String time;
    public boolean isBooked;
    public String userId;
    public String userEmail;

    public SlotModel() {}

    public SlotModel(String id, String time, boolean isBooked, String userId, String userEmail) {
        this.id = id;
        this.time = time;
        this.isBooked = isBooked;
        this.userId = userId;
        this.userEmail = userEmail;
    }
}