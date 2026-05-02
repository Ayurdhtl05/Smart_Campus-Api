package com.westminster.smartcampus.resource;

import com.westminster.smartcampus.exception.LinkedResourceNotFoundException;
import com.westminster.smartcampus.model.DataStore;
import com.westminster.smartcampus.model.Room;
import com.westminster.smartcampus.model.Sensor;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SensorResource managing the /api/v1/sensors collection.
 *
 * Provides:
 *   GET  /              — list all sensors (with optional ?type= filter)
 *   GET  /{sensorId}    — fetch a specific sensor
 *   POST /              — register a new sensor (validates roomId exists)
 *
 * Also acts as a sub-resource locator for readings:
 *   /{sensorId}/readings -> delegates to SensorReadingResource
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.INSTANCE;

    /**
     * GET /api/v1/sensors
     * Returns all sensors. Supports an optional query parameter "type"
     * (e.g., GET /api/v1/sensors?type=CO2) to filter by sensor type.
     *
     * Uses @QueryParam rather than a path segment because filtering is a
     * qualification of the collection, not a separate resource identity.
     */
    @GET
    public List<Sensor> listSensors(@QueryParam("type") String type) {
        return store.sensors().values().stream()
                .filter(s -> type == null || type.isBlank() || type.equalsIgnoreCase(s.getType()))
                .collect(Collectors.toList());
    }

    /**
     * GET /api/v1/sensors/{sensorId}
     * Fetches a specific sensor by its ID.
     */
    @GET
    @Path("/{sensorId}")
    public Sensor getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.sensors().get(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor '" + sensorId + "' not found");
        }
        return sensor;
    }

    /**
     * POST /api/v1/sensors
     * Registers a new sensor. The roomId in the request body must reference
     * an existing room — otherwise a LinkedResourceNotFoundException is thrown
     * (mapped to 422 Unprocessable Entity).
     *
     * The @Consumes(APPLICATION_JSON) annotation ensures that if a client
     * sends data in a different format (e.g., text/plain), JAX-RS will
     * automatically return 415 Unsupported Media Type without invoking this method.
     */
    @POST
    public Response createSensor(Sensor sensor) {
        // Validate required fields
        if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", 400);
            error.put("error", "Bad Request");
            error.put("message", "Sensor id is required");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        // Validate roomId is provided
        if (sensor.getRoomId() == null || sensor.getRoomId().isBlank()) {
            throw new LinkedResourceNotFoundException("roomId", "<missing>");
        }

        // Validate that the referenced room actually exists (referential integrity)
        Room room = store.rooms().get(sensor.getRoomId());
        if (room == null) {
            throw new LinkedResourceNotFoundException("roomId", sensor.getRoomId());
        }

        // Check for duplicate sensor ID
        if (store.sensors().containsKey(sensor.getId())) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", 409);
            error.put("error", "Conflict");
            error.put("message", "Sensor with id '" + sensor.getId() + "' already exists");
            return Response.status(Response.Status.CONFLICT).entity(error).build();
        }

        // Default status to ACTIVE if not provided
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }

        // Persist the sensor and link it to the room
        store.sensors().put(sensor.getId(), sensor);
        room.getSensorIds().add(sensor.getId());
        store.readings().put(sensor.getId(), new ArrayList<>());

        return Response.created(URI.create("/api/v1/sensors/" + sensor.getId()))
                       .entity(sensor)
                       .build();
    }

    /**
     * Sub-resource locator for /sensors/{sensorId}/readings.
     *
     * This method has NO @GET or @POST annotation — it returns an instance
     * of SensorReadingResource, and JAX-RS dispatches the remaining path
     * segments to that class's annotated methods.
     *
     * This pattern delegates nested logic to a separate class, keeping
     * this resource focused on sensor-level operations.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource readings(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.sensors().get(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor '" + sensorId + "' not found");
        }
        return new SensorReadingResource(sensorId);
    }
}
