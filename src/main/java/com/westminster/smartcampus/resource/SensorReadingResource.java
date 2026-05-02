package com.westminster.smartcampus.resource;

import com.westminster.smartcampus.exception.SensorUnavailableException;
import com.westminster.smartcampus.model.DataStore;
import com.westminster.smartcampus.model.Sensor;
import com.westminster.smartcampus.model.SensorReading;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sub-resource for /sensors/{sensorId}/readings.
 *
 * This class is NOT registered directly in the Application — it is
 * instantiated per-request by the sub-resource locator method in
 * SensorResource.readings(). The parent passes the sensorId via the
 * constructor so all methods here operate within that sensor's context.
 *
 * Provides:
 *   GET  / — fetch the historical readings for this sensor
 *   POST / — append a new reading (side-effect: updates parent sensor's currentValue)
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final DataStore store = DataStore.INSTANCE;
    private final String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    /**
     * GET /api/v1/sensors/{sensorId}/readings
     * Returns the full historical list of readings for this sensor.
     */
    @GET
    public List<SensorReading> getHistory() {
        List<SensorReading> history = store.readings().get(sensorId);
        if (history == null) {
            return new ArrayList<>();
        }
        return history;
    }

    /**
     * POST /api/v1/sensors/{sensorId}/readings
     * Appends a new reading to this sensor's history.
     *
     * Business rules:
     *   - If the sensor's status is not ACTIVE (e.g., MAINTENANCE),
     *     a SensorUnavailableException is thrown (mapped to 403 Forbidden).
     *   - On success, the parent Sensor's currentValue field is updated
     *     to reflect this latest reading (side-effect for data consistency).
     *
     * The method is synchronized to prevent race conditions when
     * concurrent requests try to append readings and update the parent
     * sensor's currentValue simultaneously.
     */
    @POST
    public synchronized Response addReading(SensorReading reading) {
        Sensor sensor = store.sensors().get(sensorId);
        if (sensor == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", 404);
            error.put("error", "Not Found");
            error.put("message", "Sensor '" + sensorId + "' not found");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }

        // Check sensor status — only ACTIVE sensors can accept readings
        if (!"ACTIVE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId, sensor.getStatus());
        }

        if (reading == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", 400);
            error.put("error", "Bad Request");
            error.put("message", "Reading body is required");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        // Server-managed metadata: generate ID and timestamp if not provided
        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() <= 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Append reading to history
        store.readings().computeIfAbsent(sensorId, k -> new ArrayList<>()).add(reading);

        // Side effect: update the parent sensor's currentValue for data consistency
        sensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }
}
