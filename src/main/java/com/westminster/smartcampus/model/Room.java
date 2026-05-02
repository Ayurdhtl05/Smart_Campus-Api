package com.westminster.smartcampus.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a physical room on campus that can contain sensors.
 * Each room has a unique identifier, a human-readable name,
 * a maximum capacity, and a list of sensor IDs currently assigned to it.
 */
public class Room {

    private String id;
    private String name;
    private int capacity;
    private List<String> sensorIds = new ArrayList<>();

    /** Default no-arg constructor required by Jackson for JSON deserialisation. */
    public Room() {
    }

    public Room(String id, String name, int capacity) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
    }

    // ---- Getters and Setters ----

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public List<String> getSensorIds() {
        return sensorIds;
    }

    public void setSensorIds(List<String> sensorIds) {
        this.sensorIds = sensorIds;
    }
}
