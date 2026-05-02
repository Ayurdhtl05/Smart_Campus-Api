package com.westminster.smartcampus.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory data store implemented as a singleton enum.
 *
 * JAX-RS Resource classes are per-request by default (a new instance is
 * created for every HTTP request). Therefore, persistent shared state
 * must live outside the resource classes. This enum-based singleton
 * guarantees a single shared instance across the entire application.
 *
 * All maps use ConcurrentHashMap for thread-safe concurrent access
 * without explicit synchronisation on individual put/get operations.
 */
public enum DataStore {
    INSTANCE;

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    // sensorId -> ordered list of historical readings
    private final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    /**
     * Constructor seeds the store with sample data for easier testing.
     */
    DataStore() {
        // --- Seed Rooms ---
        Room lib = new Room("LIB-301", "Library Quiet Study", 40);
        Room lab = new Room("CSE-101", "Computer Science Lab", 30);
        rooms.put(lib.getId(), lib);
        rooms.put(lab.getId(), lab);

        // --- Seed Sensors ---
        Sensor temp = new Sensor("TEMP-001", "Temperature", "ACTIVE", 21.5, "LIB-301");
        Sensor co2  = new Sensor("CO2-002",  "CO2",         "ACTIVE", 410.0, "LIB-301");
        Sensor occ  = new Sensor("OCC-003",  "Occupancy",   "MAINTENANCE", 0.0, "CSE-101");
        sensors.put(temp.getId(), temp);
        sensors.put(co2.getId(), co2);
        sensors.put(occ.getId(), occ);

        // Link sensors to their rooms
        lib.getSensorIds().add(temp.getId());
        lib.getSensorIds().add(co2.getId());
        lab.getSensorIds().add(occ.getId());

        // Initialise empty reading lists for each sensor
        readings.put(temp.getId(), new ArrayList<>());
        readings.put(co2.getId(),  new ArrayList<>());
        readings.put(occ.getId(),  new ArrayList<>());
    }

    public Map<String, Room> rooms()     { return rooms; }
    public Map<String, Sensor> sensors() { return sensors; }
    public Map<String, List<SensorReading>> readings() { return readings; }
}
