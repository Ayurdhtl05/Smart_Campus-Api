package com.westminster.smartcampus.resource;

import com.westminster.smartcampus.exception.RoomNotEmptyException;
import com.westminster.smartcampus.model.DataStore;
import com.westminster.smartcampus.model.Room;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SensorRoom Resource class managing the /api/v1/rooms path.
 *
 * Provides CRUD operations for campus rooms:
 *   GET  /           — list all rooms
 *   POST /           — create a new room
 *   GET  /{roomId}   — fetch a specific room's metadata
 *   DELETE /{roomId} — decommission a room (blocked if sensors still assigned)
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorRoomResource {

    private final DataStore store = DataStore.INSTANCE;

    /**
     * GET /api/v1/rooms
     * Returns a comprehensive list of all rooms with their full details.
     */
    @GET
    public Collection<Room> listRooms() {
        return new ArrayList<>(store.rooms().values());
    }

    /**
     * POST /api/v1/rooms
     * Creates a new room. Returns 201 Created with the room object and
     * a Location header pointing to the new resource.
     */
    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().isBlank()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", 400);
            error.put("error", "Bad Request");
            error.put("message", "Room id is required");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        if (store.rooms().containsKey(room.getId())) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", 409);
            error.put("error", "Conflict");
            error.put("message", "Room with id '" + room.getId() + "' already exists");
            return Response.status(Response.Status.CONFLICT).entity(error).build();
        }

        // Ensure sensorIds list is initialised
        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<>());
        }

        store.rooms().put(room.getId(), room);
        return Response.created(URI.create("/api/v1/rooms/" + room.getId()))
                       .entity(room)
                       .build();
    }

    /**
     * GET /api/v1/rooms/{roomId}
     * Fetches detailed metadata for a specific room.
     */
    @GET
    @Path("/{roomId}")
    public Room getRoom(@PathParam("roomId") String roomId) {
        Room room = store.rooms().get(roomId);
        if (room == null) {
            throw new NotFoundException("Room '" + roomId + "' not found");
        }
        return room;
    }

    /**
     * DELETE /api/v1/rooms/{roomId}
     * Decommissions a room. If the room still has sensors assigned,
     * a RoomNotEmptyException is thrown (mapped to 409 Conflict).
     *
     * This operation is idempotent: the first call removes the room (204),
     * subsequent calls return 404 because the room no longer exists,
     * but the server state remains the same ("room is gone").
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.rooms().get(roomId);
        if (room == null) {
            throw new NotFoundException("Room '" + roomId + "' not found");
        }

        // Business Logic Constraint: prevent deletion if sensors still assigned
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId, room.getSensorIds().size());
        }

        store.rooms().remove(roomId);
        return Response.noContent().build();
    }
}
