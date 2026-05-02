package com.westminster.smartcampus.model;

/**
 * Represents a sensor device deployed on campus.
 * Each sensor has a type (e.g. Temperature, CO2, Occupancy),
 * a status (ACTIVE, MAINTENANCE, OFFLINE), a current reading value,
 * and is linked to a specific room via roomId.
 */
public class Sensor {

    private String id;
    private String type;
    private String status;        // ACTIVE, MAINTENANCE, OFFLINE
    private double currentValue;
    private String roomId;

    /** Default no-arg constructor required by Jackson for JSON deserialisation. */
    public Sensor() {
    }

    public Sensor(String id, String type, String status, double currentValue, String roomId) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.currentValue = currentValue;
        this.roomId = roomId;
    }

    // ---- Getters and Setters ----

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
}
