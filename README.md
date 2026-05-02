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

# Conceptual Report — Answers to Specification Questions

> The questions below are answered in the order they appear in the coursework specification.

---

## Part 1.1 — Default Lifecycle of a JAX-RS Resource Class

By default, the JAX-RS runtime (Jersey included) treats each Resource class as **per-request scoped**: a brand-new instance is constructed for every incoming HTTP request and discarded once the response is returned. This means `@PathParam`, `@QueryParam`, and constructor-injected context fields can be populated safely without thread contention — each request has its own isolated object.

The architectural consequence is that **any field declared inside the resource class is request-local and cannot be used to persist data** across calls. To keep state alive between requests (the in-memory rooms, sensors, and readings), we must store it in something whose lifetime is broader than a single request. This project uses an `enum`-based singleton `DataStore` whose maps are `ConcurrentHashMap` instances, providing thread-safe concurrent access without requiring explicit locking on individual put/get operations.

Operations that mutate multiple linked structures (e.g., appending a reading **and** updating the parent sensor's `currentValue`) are protected with `synchronized` blocks to prevent race conditions that would otherwise produce lost updates or inconsistent cross-resource state. This design cleanly separates the stateless resource layer from the persistent, shared data layer.

---

## Part 1.2 — Why is HATEOAS Important?

HATEOAS (Hypermedia As The Engine Of Application State) embeds links to related resources directly inside API responses. Our discovery endpoint at `GET /api/v1` returns a `resources` map containing URIs for `rooms`, `sensors`, and `sensorReadings`, giving clients a runtime-discoverable map of the entire API surface.

For client developers, the benefits over relying solely on static documentation are:

- **Discoverability** — clients can crawl the API starting from a single root URL, discovering all available operations dynamically.
- **Loose coupling and evolvability** — if the server changes its URI structure, clients that follow links (rather than hard-coding paths) automatically adapt without code changes.
- **State-driven navigation** — the available next actions (e.g., "delete room", "add reading") can be expressed as links, so the client does not need to re-implement business rules to determine what is currently permitted.
- **Reduced documentation drift** — the API itself becomes the authoritative source of truth, reducing the risk that documentation falls out of sync with the actual implementation.

---

## Part 2.1 — IDs vs Full Objects in Collection Responses

Returning **only IDs** minimises payload size, which is attractive on bandwidth-constrained networks and for very large collections. However, every consumer that needs actual data must then issue an additional request per item, resulting in the "N+1 request problem" that multiplies round trips and total latency.

Returning **full objects** is heavier on the wire but gives the client everything in a single call, simplifying client-side processing and enabling effective caching. 

In this project, `GET /rooms` returns full room objects because the dataset is small and clients typically need the human-readable `name`, `capacity`, and `sensorIds` list immediately. For very large datasets, a practical compromise would be pagination combined with a "summary" representation (id + name only) with optional `?expand=sensors`-style parameters to allow clients to control the level of detail.

---

## Part 2.2 — Is DELETE Idempotent?

Yes, the `DELETE` operation in this implementation is **idempotent**. The HTTP specification defines an idempotent method as one whose effect on server state is the same whether invoked once or many times.

In this implementation:
- The **first** `DELETE /rooms/{id}` succeeds, removes the room, and returns `204 No Content`.
- A **subsequent identical** `DELETE` for the same room finds no matching resource and the `NotFoundExceptionMapper` returns `404 Not Found`.

The **state of the server** — namely, "the room is gone" — is identical after the first call and after any number of repeated calls. The response status codes may differ (204 vs 404), but idempotency is defined in terms of **server state**, not response body. Therefore, the implementation fully respects the idempotency contract of the HTTP `DELETE` method.

---

## Part 3.1 — Consequences of `@Consumes(MediaType.APPLICATION_JSON)`

The `@Consumes` annotation declares which `Content-Type` request bodies the method is willing to accept. When a client sends data with a content type of `text/plain` or `application/xml` to a method annotated with `@Consumes(MediaType.APPLICATION_JSON)`, the JAX-RS runtime **never invokes the method at all**. Instead, it short-circuits the request dispatch and automatically returns **HTTP 415 Unsupported Media Type**.

This is a content-negotiation safeguard — the framework refuses to attempt deserialisation with a `MessageBodyReader` that was not designed for that media type. As a result:
- Business logic is never exposed to malformed or unexpected payloads.
- The matching `MessageBodyReader` (Jackson, in our case) is guaranteed to always receive well-formed JSON input.
- Developers do not need to write manual content-type validation code inside every endpoint.

---

## Part 3.2 — `@QueryParam` vs Path-Embedded Filter

Placing the filter value in a URL path segment (e.g., `/sensors/type/CO2`) implies that "sensors of type CO2" is a distinct, independently addressable sub-resource. However, it is simply a **filtered view** over the same `/sensors` collection. Using `@QueryParam` (e.g., `?type=CO2`) is the superior approach because:

- **Preserves the collection identity** — there is one canonical `/sensors` resource; query parameters merely shape its representation without creating artificial sub-resources.
- **Composes naturally** — multiple optional filters can be combined (`?type=CO2&status=ACTIVE`) without causing a combinatorial explosion of path definitions.
- **Plays well with caching, bookmarks, and analytics** — query strings are standard, well-understood URL components that proxies, caches, and logging tools handle correctly.
- **Keeps the routing table small and the API self-documenting** — fewer path definitions make the API easier to understand and maintain.

The REST design principle is clear: path segments should identify **resources**, while query parameters should **qualify** or **filter** them. Filtering, sorting, and pagination are textbook qualifiers, making `@QueryParam` the idiomatic and recommended choice.

---

## Part 4.1 — Benefits of the Sub-Resource Locator Pattern

A sub-resource locator is a method on a parent resource that **returns a new resource object** rather than being a direct HTTP endpoint. JAX-RS then dispatches the remaining path segments against that returned object. In this project, `SensorResource.readings(sensorId)` returns a freshly constructed `SensorReadingResource` instance carrying the parent `sensorId` in its constructor.

The architectural benefits include:

- **Separation of concerns** — readings logic lives in `SensorReadingResource`; the parent `SensorResource` class is not bloated with deeply nested handlers.
- **Reusability** — the same sub-resource class could potentially be accessed from multiple parent resources if needed.
- **Per-request context capture** — the parent passes `sensorId` (and validates that the sensor exists) before constructing the sub-resource, eliminating repetitive validation in every nested method.
- **Scalability of the codebase** — each level of nesting is an independent, testable class. Large APIs avoid a single monolithic controller with dozens of path overloads, making the code easier to read, test, and maintain.

---

## Part 5.2 — Why 422 Instead of 404?

HTTP **404 Not Found** semantically describes a missing **target URI** — the resource addressed by the request URL does not exist. When a client sends `POST /api/v1/sensors` with a JSON body whose `roomId` references a non-existent room, the request URI (`/api/v1/sensors`) is perfectly valid and the request body is syntactically well-formed JSON. The problem lies in the **referential integrity inside the payload**, not the URL.

HTTP **422 Unprocessable Entity** was defined precisely for this scenario: the server understood the request, parsed the body successfully, but cannot process it due to **semantic errors** in the content. Using 422:
- Disambiguates the failure for the client: a 404 on a POST is ambiguous (does the collection itself not exist?), whereas a 422 with a `field`/`rejectedValue` payload tells the client precisely which referenced entity is missing.
- Follows the principle that different error conditions should use different status codes to enable programmatic error handling by clients.

---

## Part 5.4 — Risks of Exposing Java Stack Traces

Stack traces are highly informative debugging artefacts, which is exactly why exposing them externally is a significant security risk. From an attacker's perspective, leaked stack traces reveal:

- **Exact framework, library, and JVM versions** — enabling targeting of known CVEs (Common Vulnerabilities and Exposures) specific to those versions.
- **Internal package and class names** — hinting at architecture and business logic (e.g., `UserRepository.findByEmail`), which helps in crafting targeted attacks.
- **File system paths** — baked into class-loading messages, assisting in path-traversal probes.
- **Database driver names or JDBC URLs** — visible in database-related exceptions, potentially revealing connection details.
- **Input types that crash the system** — which feeds fuzzing and injection attack strategies.
- **Secrets, tokens, or query fragments** — sometimes echoed back in exception messages.

Beyond hostile exploitation, leaking traces is also an information-disclosure violation under most security baselines (OWASP A04: Insecure Design, A05: Security Misconfiguration, ISO 27001). Our `GenericExceptionMapper<Throwable>` logs the full stack trace **server-side** for operators while returning only a generic, non-revealing JSON body to the external client.

---

## Part 5.5 — Why Use Filters for Cross-Cutting Concerns?

Logging, authentication, request timing, and similar concerns are **orthogonal** to business logic — they apply uniformly to every endpoint. Manually inserting `Logger.info()` calls inside every single resource method has several significant drawbacks:

- **Code duplication and drift** — every developer might log in a slightly different format; some methods may be forgotten entirely.
- **Coupling** — business code becomes tangled with infrastructure concerns, hurting both readability and testability.
- **Centralised change cost** — switching to structured JSON logs, adding correlation IDs, or adjusting log levels requires editing every resource method individually.

A JAX-RS filter (`ContainerRequestFilter` / `ContainerResponseFilter`) intercepts the request/response pipeline **once** for the entire application. Resource methods remain focused exclusively on their domain task, while logging, metrics, security headers, and other cross-cutting concerns are configured in **one place** and applied **consistently** to every call — including endpoints added in the future. This follows the Single Responsibility Principle and promotes maintainable, clean architecture.
