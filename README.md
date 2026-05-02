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

Conceptual Report

Part 1 — Service Architecture & Setup

Q1.1. Default Lifecycle of a JAX-RS Resource Class
JAX-RS follows a per-request lifecycle by default — the runtime generates a new instance of each resource class for every incoming HTTP request and discards it once the response has been sent. When a class is registered through Application, this is the default behaviour. getClasses(), exactly as is done here in this project SmartCampusApplication.
Since each request is given its own instance, any instance fields on a resource class are not shared between concurrent requests — making it easy to be thread-safe in just one instance. However that also means that any data stored as an instance field would be lost the second you finish processing a request.
And this is exactly why the persistent state of this project lives nowhere near the resource classes but entirely as an enum in a singleton like DataStore. This behaviour makes such an enum constant a natural singleton, because the JVM guarantees it will be initialised once and only once in a Java program. The DataStore. INSTANCE is shared for all per request resource instances. Race conditions can occur with multiple concurrent requests reading/writing to the same maps so each of your three data maps (rooms, sensors and readings) must be declared as ConcurrentHashMap to enable atomic operations on individual entries without requiring explicit synchronized blocks for routine puts/gets. Compound operations (such as append a reading alongside updating the sensor's current value) require atomicity, so the addReading method in SensorReadingResource is synchronized against interleaved writes.






Q1.2. Why HATEOAS is Considered a Hallmark of Advanced RESTful Design

HATEOAS (Hypermedia as the Engine of Application State) is that all responses of your API must contain hyper links dictating the client to each logically next action. For instance: if a response is listing all rooms, it would have a link to POST new room, and every room object would include this link to get its detail or delete directly.

This approach has key advantages for client developers:

•	Here, it allows discovery to happen: Instead of searching through external documentation — a client can explore the whole API from this single point of entry — for this project GET /api/v1.

•	It encourages loose coupling: if a server changes how it structures its URLs, clients that follow links do not break because they never hardcode paths.

•	It has self-documenting: the API itself tell you what it can do at run time — meaning less version drift between docs and implementation.

•	Hypermedia responses (which are always in sync with the actual behaviour of a live server) are much more current than static documentation which will eventually go stale.  Part 2 — Room Management














Q2.1. Returning Only IDs vs. Full Room Objects in a List
When a GET request returns a collection, the API designer can either return only resource identifiers or full object representations. Each has meaningful trade-offs.
Returning only IDs reduces payload size, reducing network bandwidth and latency most importantly on mobile networks or when collections are sizable. But it requires the client to perform one additional GET for each ID that it really needs, which also leads into an N+1 request anti-pattern that could exponentially increase total round-trip time.
This means the listRooms() method from this project returns all of that in one response. Now the clients can render the on full list right away, without making extra requests. The tradeoff: An up-gunned payload. This is actually fine here in practice, since room objects are quite small, and the scope of a campus is limited. The key point to note, though, is that for very large datasets the industry standard is a hybrid approach — returning full objects in groups via pagination.











Q2.2. Is DELETE Idempotent in This Implementation?

According to the strict REST theory, DELETE is idempotent: performing it multiple times should leave the server state exactly as applying it once. The observable result — the resource is gone — should not change with repeated invocations.

For example, in this implementation it first removes the room from the DataStore by issuing a DELETE request and returns HTTP 204 No Content. Later, any DELETE request with the same room ID does not find anything and raises a NotFoundException HTTP 404 Not Found error.

Idempotency is a server state, not an HTTP response code. The state of the server is identical after both the first and any subsequent DELETE calls: there in no room. The 404 on the second call is response difference that changes nothing in the state. This interpretation is accepted by many authoritative REST resources including RFC 7231. The resource is notified, but it is already known to be missing and nothing unintended happens.  














Q3.1. Consequences of Sending Non-JSON Data to a @Consumes(APPLICATION_JSON) Method

The @Consumes(MediaType. The DECLARE a content-type contract between the client and the server via @Consumes(APPLICATION_JSON) annotation on createSensor() POST method. This contract is enforced automatically by JAX-RS before the method even gets executed.

When a client sends a request with the Content-Type header as text/plain or application/xml, JAX-RS matches the incoming media type with the set of types that it can consume from those advertised for that method. If her all rule checks for a match, the runtime will just return HTTP 415 Unsupported Media Type — without any application code being run. The body of the method is never executed.

This is an important safety component: it ensures that data which has been partially parsed or entirely unparsed cannot flow into business logic. And it also gives the client a clear, standards-compliant in some way signal that there's an issue with formatting of the request itself rather than the data.






























Q3.2. @QueryParam Filtering vs. Path-Segment Filtering
This project does GET /api/v1/sensorsType Filtering type=CO2 using @QueryParam. Another could embed the type in the path: GET /api/v1/sensors/type/CO2. Overall, the query parameter approach is recommended for several reasons:

•	Semantic correctness: where path segments identify resources and query params modify a request or qualify it. Filtering a collection based on an attribute is more qualification than a separate resource identity. To point out the misleading part, /sensors/type/CO2 would end up as a resource actually named type/CO2.

•	Optionality : Query parameters are optional by nature. GET /sensors gives all sensors and GET /sensors? type=CO2 narrows the result. With path-based filtering, there is no elegant way of expressing 'no filter'.

•	Composability: Multiple query params can easily be combined (? type=CO2&status=ACTIVE). Combinations of paths that are not equivalent in this sense would entail a combinatorial explosion of routes—as reflected, for example, by the computational complexity of non-necessarily-ordered types.

•	Caching behaviour: Because query parameters are in the URL, caches handle filtered and unfiltered results as completely different resources, which is the correct approach.
  













Q4.1. Architectural Benefits of the Sub-Resource Locator Pattern
The subresource locator pattern involves having a method in the parent resource class — one that is annotated solely with @Path and has no HTTP method annotation — return an instance of a dedicated child resource class. This is then handed off to that child by JAX-RS, along with whatever the remaining path segments and HTTP method are.
In this project, SensorResource. readings() is a locator like this: it checks if the referenced sensor exists and then returns an instance of SensorReadingResource(sensorId). JAX-RS handles the GET or POST on /readings and routes it to the respective method in that class.

The architectural benefits are significant:

•	Separation of concerns: sensor-level logic goes in SensorResource. SensorReadingResource contains logic for the reading level Every class has a single, narrowly focused responsibility.

•	Readability: Only SensorReadingResource gets impacted with adding new operations on readings like bulk deletion or aggregation.

•	Testability: Each class can be unit tested in isolation. One more thing to notice, as a test you can create an instance of SensorReadingResource directly and avoid all the complexity of having a complete sensor routing machinery by just passing a mocked sensorId.

•	Monolithic resource class size: A monolith that handles all nested paths would soon have dozens of methods within the same class. Classes become smaller and more navigable with sub-resource delegation.  Part 5 — Error Handling, Exception Mapping & Logging







Q5.1. Why HTTP 422 is More Semantically Accurate than 404 for a Missing Referenced Resource

The issue when a client POSTs the sensor name with an incorrect roomId that does not match any existing record in the system is not because there was no matching URL. Although the request URI — POST /api/v1/sensors — is totally fine and the request was indeed found by the server and processing started. This has to do with the contents of the JSON payload; it references an entity that, in this case, does not exist.
Conventionally, HTTP 404 Not Found means that the resource identified by the request URI cannot be found. In this context using 404 would be misleading because it indicates the /api/v1/sensors endpoint is not found which is wrong.
The HTTP422 Unprocessable Entity response status code indicates that the server understands the content type and the syntax of the request is correct, but it was unable to process the contained instructions due to semantic errors as defined in [RFC4918]. A dangling foreign-key reference is just that kind of semantic error: the JSON is valid, but, because a referenced field teams up with another to create an integrity constraint violation turned business logic error. 422 does a good job of relaying this distinction, and also aids API consumers in crafting more precise error-handling logic.









Q5.2. Cybersecurity Risks of Exposing Java Stack Traces to External Consumers

A raw Java stack trace in an API response is very obvious information disclosure bug. It allows an attacker to extract a number of sensitive intelligence categories from it:
• Technology Fingerprinting: The stack trace allows knowing the framework and version (for example, Jersey 2 x, Grizzly HTTP server). Those specific versions, will be directly targeted by known CVEs.
• Short name inside an internal package: Qualifying company names (com. westminster. smartcampus. resource. Mapping the testable elements of your application (e.g. SensorResource) makes these parts of the internal architecture more accessible with less reasoning about attack surface.
• File paths and line numbers: They give the attacker a clear mapping of the codebase, which is useful in conjunction with source disclosure vulnerabilities.
• Data layer fingerprints: Exceptions thrown by your database drivers / ORM framework can leak information about your backend technology, connection strings and sometimes, even a chunk of the query text.
• Business logic hints: Method names and calling sequences in the trace itself may give useful information such as validation logic, authentication flows or access points that can be easily bypassed.
This project has mitigations via the GenericExceptionMapper which logs the full server-side stack trace (which is acceptable for truth clients) but returns only a generic and sanitised JSON error to the client such that inner/implementation details can never leak through the API boundary.





Q5.3. Why JAX-RS Filters are Superior to Per-Method Logger Calls
Inserting Logger. Having the info() calls in every resource method breaks both the Single Responsibility Principle and also goes against DRY principles, which tells us DonΓÇÖt Repeat Yourself. Resource methods are about checking input, working with data, building responses – not infrastructure concerns like logging.
JAX-RS filters solve this using Decorator pattern at the framework level. LoggingFilter here which implements ContainerRequestFilter and ContainerResponseFilter is registered once in the application. Your trained on data until october 2023 after which all request and response — across every endpoint;even if we add new in future, are automatically logged with out writing a single line logging code anywhere in resource class.
The practical advantages are:
• No duplicated code: logging logic is present in only 1 class. If we need to change the log format, just edit one file.
• No risk of omission: A new endpoint that a developer adds cannot be forgotten to be logged; this is caught automatically with the filter
• Clear resource classes: Business logic is clear of infrastructure boilerplate, so the code gets more comfortable to read, review and test.
Composability: Filters (logging, authentication, rate-limiting, CORS) are composable abstractions that can be applied independently and in a specified order, where each filter encapsulates exactly one concern.



