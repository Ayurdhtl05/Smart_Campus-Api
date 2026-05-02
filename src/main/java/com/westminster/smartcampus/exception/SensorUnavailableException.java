package com.westminster.smartcampus.exception;

/**
 * Custom exception thrown when a client attempts to POST a reading
 * to a sensor that is not in ACTIVE status (e.g., MAINTENANCE or OFFLINE).
 *
 * Mapped to HTTP 403 Forbidden by SensorUnavailableExceptionMapper.
 */
public class SensorUnavailableException extends RuntimeException {

    private final String sensorId;
    private final String status;

    public SensorUnavailableException(String sensorId, String status) {
        super("Sensor '" + sensorId + "' is currently in '" + status
              + "' mode and cannot accept new readings.");
        this.sensorId = sensorId;
        this.status = status;
    }

    public String getSensorId() {
        return sensorId;
    }

    public String getStatus() {
        return status;
    }
}
