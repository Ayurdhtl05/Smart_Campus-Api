package com.westminster.smartcampus.exception;

/**
 * Custom exception thrown when a client attempts to delete a Room
 * that still has active Sensors assigned to it.
 *
 * Mapped to HTTP 409 Conflict by RoomNotEmptyExceptionMapper.
 */
public class RoomNotEmptyException extends RuntimeException {

    private final String roomId;
    private final int sensorCount;

    public RoomNotEmptyException(String roomId, int sensorCount) {
        super("Room '" + roomId + "' cannot be deleted: " + sensorCount
              + " sensor(s) still assigned. Remove all sensors before deleting this room.");
        this.roomId = roomId;
        this.sensorCount = sensorCount;
    }

    public String getRoomId() {
        return roomId;
    }

    public int getSensorCount() {
        return sensorCount;
    }
}
