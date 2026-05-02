# Smart Campus — Sensor & Room Management API

**Module:** 5COSC022W Client-Server Architectures (2025/26)  
**Stack:** Java 11 · Maven · JAX-RS (Jersey 2.41) · Embedded Grizzly HTTP Server  
**Storage:** In-memory (`ConcurrentHashMap`) — no database, as required by the specification.

This project exposes a versioned RESTful API under `/api/v1` for managing campus **Rooms**, the **Sensors** deployed inside them, and the historical **Readings** each sensor produces. It demonstrates resource nesting via sub-resource locators, custom exception mappers, request/response logging filters, and HATEOAS-style discovery.

---

## 1. API Design Overview

| Resource | Path | Operations |
|---|---|---|
| Discovery | `GET /api/v1` | API metadata + HATEOAS links |
| Rooms | `GET /api/v1/rooms` | List all rooms |
| Rooms | `POST /api/v1/rooms` | Create a new room |
| Rooms | `GET /api/v1/rooms/{roomId}` | Get room details |
| Rooms | `DELETE /api/v1/rooms/{roomId}` | Delete a room (blocked if sensors assigned) |
| Sensors | `GET /api/v1/sensors` | List all sensors (supports `?type=` filter) |
| Sensors | `GET /api/v1/sensors/{sensorId}` | Get sensor details |
| Sensors | `POST /api/v1/sensors` | Register a new sensor (validates roomId) |
| Readings | `GET /api/v1/sensors/{sensorId}/readings` | Get reading history |
| Readings | `POST /api/v1/sensors/{sensorId}/readings` | Add a reading (updates parent currentValue) |

### Error Model

All errors return JSON in a consistent format:
```json
{
  "status": <int>,
  "error": "...",
  "message": "..."
}
```

| Status | Exception | Scenario |
|---|---|---|
| 404 Not Found | `NotFoundException` | Resource does not exist |
| 409 Conflict | `RoomNotEmptyException` | DELETE room with active sensors |
| 422 Unprocessable Entity | `LinkedResourceNotFoundException` | POST sensor with invalid roomId |
| 403 Forbidden | `SensorUnavailableException` | POST reading to non-ACTIVE sensor |
| 500 Internal Server Error | `GenericExceptionMapper<Throwable>` | Any unhandled runtime error |

A `LoggingFilter` (implements both `ContainerRequestFilter` and `ContainerResponseFilter`) logs every request and response.

---

## 2. Build & Run

### Prerequisites
- **JDK 11** or higher (`java -version`)
- **Maven 3.6+** (`mvn -version`) — NetBeans includes Maven at `<NetBeans>/java/maven/bin/mvn`

### Steps

```bash
# 1. Clone the repository
git clone <your-repo-url> smart-campus-api
cd smart-campus-api

# 2. Build (produces target/smart-campus-api.jar — runnable fat-jar)
mvn clean package

# 3. Run the server
java -jar target/smart-campus-api.jar

# Server starts on http://localhost:8080/api/v1
```

**From NetBeans:** Right-click the project → Run, or run `com.westminster.smartcampus.app.Main` directly.

The store is **pre-seeded** with:
- 2 rooms: `LIB-301` (Library Quiet Study), `CSE-101` (Computer Science Lab)
- 3 sensors: `TEMP-001` (Temperature, ACTIVE), `CO2-002` (CO2, ACTIVE), `OCC-003` (Occupancy, MAINTENANCE)

---

## 3. Sample `curl` Commands

```bash
# 1) Discovery endpoint — returns API metadata and HATEOAS links
curl -s http://localhost:8080/api/v1

# 2) List all rooms
curl -s http://localhost:8080/api/v1/rooms

# 3) Create a new room
curl -s -X POST http://localhost:8080/api/v1/rooms \
     -H "Content-Type: application/json" \
     -d "{\"id\":\"ENG-204\",\"name\":\"Engineering Workshop\",\"capacity\":25}"

# 4) Get a specific room
curl -s http://localhost:8080/api/v1/rooms/LIB-301

# 5) Create a new sensor linked to an existing room
curl -s -X POST http://localhost:8080/api/v1/sensors \
     -H "Content-Type: application/json" \
     -d "{\"id\":\"HUM-004\",\"type\":\"Humidity\",\"status\":\"ACTIVE\",\"currentValue\":45.0,\"roomId\":\"ENG-204\"}"

# 6) Filter sensors by type (query parameter)
curl -s "http://localhost:8080/api/v1/sensors?type=CO2"

# 7) Post a reading (updates parent sensor's currentValue)
curl -s -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
     -H "Content-Type: application/json" \
     -d "{\"value\":22.7}"

# 8) Get reading history
curl -s http://localhost:8080/api/v1/sensors/TEMP-001/readings

# 9) Try to delete room with sensors -> 409 Conflict
curl -s -X DELETE http://localhost:8080/api/v1/rooms/LIB-301

# 10) Try to create sensor with non-existent roomId -> 422 Unprocessable Entity
curl -s -X POST http://localhost:8080/api/v1/sensors \
     -H "Content-Type: application/json" \
     -d "{\"id\":\"X-1\",\"type\":\"Temperature\",\"status\":\"ACTIVE\",\"roomId\":\"DOES-NOT-EXIST\"}"

# 11) Try to POST reading to MAINTENANCE sensor -> 403 Forbidden
curl -s -X POST http://localhost:8080/api/v1/sensors/OCC-003/readings \
     -H "Content-Type: application/json" \
     -d "{\"value\":5}"

# 12) Delete a room (first remove its sensors or use an empty room)
curl -s -X DELETE http://localhost:8080/api/v1/rooms/ENG-204
```

---

## 4. Project Structure

```
src/main/java/com/westminster/smartcampus/
├── app/
│   ├── Main.java                          # Embedded Grizzly bootstrap
│   └── SmartCampusApplication.java        # @ApplicationPath("/api/v1")
├── model/
│   ├── Room.java                          # Room POJO
│   ├── Sensor.java                        # Sensor POJO
│   ├── SensorReading.java                 # SensorReading POJO
│   └── DataStore.java                     # Thread-safe in-memory store (singleton enum)
├── resource/
│   ├── DiscoveryResource.java             # GET /api/v1
│   ├── SensorRoomResource.java            # /rooms (CRUD)
│   ├── SensorResource.java                # /sensors (+ sub-resource locator)
│   └── SensorReadingResource.java         # /sensors/{id}/readings (sub-resource)
├── exception/
│   ├── RoomNotEmptyException.java         # Room has active sensors
│   ├── LinkedResourceNotFoundException.java # Referenced resource missing
│   └── SensorUnavailableException.java    # Sensor not ACTIVE
├── mapper/
│   ├── RoomNotEmptyExceptionMapper.java   # → 409 Conflict
│   ├── LinkedResourceNotFoundExceptionMapper.java # → 422 Unprocessable Entity
│   ├── SensorUnavailableExceptionMapper.java # → 403 Forbidden
│   ├── NotFoundExceptionMapper.java       # → 404 Not Found
│   └── GenericExceptionMapper.java        # → 500 Internal Server Error (catch-all)
└── filter/
    └── LoggingFilter.java                 # Request/response logging
```

---

