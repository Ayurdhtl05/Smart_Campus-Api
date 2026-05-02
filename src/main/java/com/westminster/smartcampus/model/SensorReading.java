package com.westminster.smartcampus.model;

import java.util.UUID;

/**
 * Represents a single historical reading from a sensor.
 * Each reading has a unique ID, a timestamp (epoch millis),
 * and the recorded value.
 */
public class SensorReading {

    private String id;
    private long timestamp;
    private double value;

    /** Default no-arg constructor required by Jackson for JSON deserialisation. */
    public SensorReading() {
    }

    public SensorReading(double value) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.value = value;
    }

    // ---- Getters and Setters ----

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
