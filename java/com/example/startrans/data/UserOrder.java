package com.example.startrans.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class UserOrder implements Comparable <UserOrder>{

    @PrimaryKey
    private int id;
    private String from;
    private String to;
    private String dateAndTime;
    private String typeOfVehicle;
    private String weight;
    private String size;
    private String adds;

    public UserOrder () {};

    public UserOrder(int id, String from, String to, String dateAndTime, String typeOfVehicle, String weight, String size, String adds) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.dateAndTime = dateAndTime;
        this.typeOfVehicle = typeOfVehicle;
        this.weight = weight;
        this.size = size;
        this.adds = adds;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getDateAndTime() {
        return dateAndTime;
    }

    public void setDateAndTime(String dateAndTime) {
        this.dateAndTime = dateAndTime;
    }

    public String getTypeOfVehicle() {
        return typeOfVehicle;
    }

    public void setTypeOfVehicle(String typeOfVehicle) {
        this.typeOfVehicle = typeOfVehicle;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getAdds() {
        return adds;
    }

    public void setAdds(String adds) {
        this.adds = adds;
    }

    @Override
    public int compareTo(UserOrder o) {
        return 0;
    }
}
