package com.westminster.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Root "Discovery" endpoint at GET /api/v1.
 *
 * Returns a JSON object providing essential API metadata including
 * versioning info, administrative contact details, and a HATEOAS-style
 * map of primary resource collection URIs so clients can navigate the
 * API programmatically without hard-coding paths.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("apiName", "Smart Campus Sensor & Room Management API");
        body.put("version", "1.0.0");
        body.put("description", "RESTful API for managing campus rooms, sensors, and sensor readings");

        // Administrative contact details
        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("team", "Smart Campus Backend Team");
        contact.put("email", "smartcampus@westminster.ac.uk");
        body.put("contact", contact);

        // HATEOAS-style map of primary resource collections
        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("self", "/api/v1");
        resources.put("rooms", "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");
        resources.put("sensorReadings", "/api/v1/sensors/{sensorId}/readings");
        body.put("resources", resources);

        return Response.ok(body).build();
    }
}
