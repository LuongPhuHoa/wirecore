# WireCore

A backend HTTP framework built from scratch in pure Java — no Spring, no Jetty, no external dependencies. Built for learning how HTTP servers actually work under the hood.

## Constraints

- **No frameworks** — no Spring Boot, no Netty, no Jetty
- **Core Java only** — `java.net`, `java.io`, `java.util.concurrent`, and standard library
- **Readable code** — clarity over cleverness

## Project Structure (Target)

```
src/
├── App.java                          # Entry point — bootstraps the server
├── server/
│   ├── WireServer.java               # Core server — accepts socket connections
│   ├── ConnectionHandler.java        # Reads/writes on a single connection
│   ├── HttpRequest.java              # Parsed request (method, path, headers, body)
│   ├── HttpResponse.java             # Response builder (status, headers, body)
│   └── HttpParser.java               # Parses raw bytes into HttpRequest
├── routing/
│   ├── Router.java                   # Maps (method + path) → handler
│   ├── Route.java                    # Single route definition
│   └── RouteHandler.java             # @FunctionalInterface for handlers
├── middleware/
│   ├── Middleware.java               # @FunctionalInterface for middleware
│   └── MiddlewareChain.java          # Executes middleware + final handler
├── thread/
│   └── ThreadPoolManager.java        # Wraps ExecutorService configuration
├── error/
│   ├── HttpException.java            # Custom exception with status code
│   └── ErrorHandler.java             # Global error-to-response mapper
└── util/
    ├── HttpStatus.java               # Enum of HTTP status codes
    ├── MimeType.java                 # Content-Type constants
    └── JsonUtil.java                 # Minimal JSON serialization (Phase 7)
```

## Naming Conventions

| Element    | Convention         | Example                          |
| ---------- | ------------------ | -------------------------------- |
| Classes    | `PascalCase`       | `HttpRequest`, `RouteHandler`    |
| Methods    | `camelCase`        | `getPath()`, `addRoute()`        |
| Packages   | all lowercase      | `server`, `routing`, `middleware` |
| Constants  | `UPPER_SNAKE_CASE` | `HTTP_200_OK`, `TEXT_PLAIN`      |

---

# Development Roadmap

## Phase 1 — The Listening Socket

> **Goal:** Get a Java process to listen on a port and reply to HTTP requests with hardcoded text.

### Concepts

- TCP sockets — OS-level file descriptors that send/receive byte streams
- Client-server model — `ServerSocket.accept()` blocks until a connection arrives
- Streams — `InputStream` reads bytes in, `OutputStream` writes bytes out
- HTTP over TCP — HTTP is just structured text over a TCP connection

### Tasks

| #   | Task                              | Details                                                                                |
| --- | --------------------------------- | -------------------------------------------------------------------------------------- |
| 1.1 | Create `WireServer.java`          | Constructor takes a `port`. Has a `start()` method.                                    |
| 1.2 | Open a `ServerSocket` on the port | `new ServerSocket(port)` — binds the OS socket.                                        |
| 1.3 | Accept one connection             | `Socket client = serverSocket.accept()` — blocks and waits.                            |
| 1.4 | Read the raw input stream         | Read bytes from `client.getInputStream()`, convert to `String`, print to console.      |
| 1.5 | Send a hardcoded response         | Write a valid HTTP response string to `client.getOutputStream()`.                      |
| 1.6 | Loop to accept more connections   | Wrap accept logic in `while(true)` so the server keeps running.                        |
| 1.7 | Wire it up in `App.java`          | `new WireServer(8080).start()`                                                         |

### The hardcoded HTTP response

```java
String response = "HTTP/1.1 200 OK\r\n"
    + "Content-Type: text/plain\r\n"
    + "\r\n"
    + "Hello from WireCore!";
```

Key detail: `\r\n` line endings are mandatory per HTTP spec (RFC 7230). The blank line (`\r\n\r\n`) separates headers from body.

### Checkpoint

Run the server, open `http://localhost:8080` in a browser.

- Browser shows "Hello from WireCore!"
- Terminal prints the raw HTTP request the browser sent

```bash
curl -v http://localhost:8080
```

### Common Mistakes

- **Forgetting to close sockets/streams** — use try-with-resources
- **Using `\n` instead of `\r\n`** — browsers may reject the response
- **Not flushing `OutputStream`** — response may never arrive
- **Handling one request then exiting** — need a `while(true)` loop around `accept()`

---

## Phase 2 — Parsing HTTP Requests

> **Goal:** Turn raw HTTP text into a structured `HttpRequest` object with method, path, headers, and body.

### Concepts

- HTTP message format — request line, headers, blank line, body
- Line-by-line text parsing
- GET vs POST — GET has no body, POST needs `Content-Length` to know how many bytes to read
- Reading exactly N bytes for the body (not more, not less)

### Tasks

| #   | Task                        | Details                                                                                                     |
| --- | --------------------------- | ----------------------------------------------------------------------------------------------------------- |
| 2.1 | Design `HttpRequest`        | Fields: `String method`, `String path`, `Map<String, String> headers`, `String body`, `Map<String, String> queryParams` |
| 2.2 | Create `HttpParser.java`    | Static method: `HttpRequest parse(InputStream input)`                                                       |
| 2.3 | Parse the request line      | Split `"GET /users?id=5 HTTP/1.1"` → method=`GET`, path=`/users`, queryParams=`{id: 5}`                    |
| 2.4 | Parse headers               | Read lines until blank line. Split each on first `:`. Trim values. Store in map with lowercase keys.        |
| 2.5 | Parse body (if present)     | Read `Content-Length` header. Read exactly that many bytes from stream.                                      |
| 2.6 | Build `HttpResponse.java`   | Builder-style: `response.status(200).header("Content-Type", "text/plain").body("OK")` → has `toBytes()`     |
| 2.7 | Create `ConnectionHandler`  | Replaces inline logic: parses request, logs it, builds response with `HttpResponse`.                        |

### Expected parse output

For `GET /users?name=john HTTP/1.1`:

```
HttpRequest {
  method = "GET"
  path = "/users"
  queryParams = { "name" -> "john" }
  headers = { "host" -> "localhost:8080", "user-agent" -> "curl/7.x" }
  body = ""
}
```

### Checkpoint

```bash
curl -v http://localhost:8080/hello?name=world
```

Server logs the parsed request object. Response includes the path and query params.

### Common Mistakes

- **Reading body with `BufferedReader.readLine()`** — blocks waiting for more data. Use `InputStream.read(byte[], 0, contentLength)` instead
- **Assuming headers are case-sensitive** — HTTP headers are case-insensitive per spec. Store keys with `.toLowerCase()`
- **Trying to read a body on GET requests** — check `Content-Length` exists before reading
- **Using `Scanner`** — buffers too aggressively and eats bytes you need later

---

## Phase 3 — Routing System

> **Goal:** Map URL paths + HTTP methods to handler functions. `/users` GET does something different than `/users` POST.

### Concepts

- Functional interfaces and lambdas in Java
- Strategy pattern — handlers are interchangeable behavior
- How frameworks like Spring map annotations to methods under the hood
- Path matching (exact match first, then parameterized)

### Tasks

| #   | Task                          | Details                                                                                      |
| --- | ----------------------------- | -------------------------------------------------------------------------------------------- |
| 3.1 | Create `RouteHandler`         | `@FunctionalInterface` with method `void handle(HttpRequest req, HttpResponse res)`          |
| 3.2 | Create `Route` class          | Holds `String method`, `String path`, `RouteHandler handler`                                 |
| 3.3 | Create `Router` class         | Has `addRoute(method, path, handler)` and convenience methods `get()`, `post()`              |
| 3.4 | Implement route matching      | `Router.resolve(method, path)` returns the matching `RouteHandler` or null                   |
| 3.5 | Add 404 handling              | If `resolve()` returns null, respond with `404 Not Found`                                    |
| 3.6 | Wire into `App.java`          | Register routes with a fluent API                                                            |

### App.java after this phase

```java
public class App {
    public static void main(String[] args) {
        Router router = new Router();

        router.get("/", (req, res) -> {
            res.status(200).body("Welcome to WireCore!");
        });

        router.get("/hello", (req, res) -> {
            String name = req.getQueryParam("name");
            res.status(200).body("Hello, " + (name != null ? name : "world") + "!");
        });

        router.post("/echo", (req, res) -> {
            res.status(200).body("You sent: " + req.getBody());
        });

        new WireServer(8080, router).start();
    }
}
```

### Checkpoint

```bash
curl http://localhost:8080/                         # → Welcome to WireCore!
curl http://localhost:8080/hello?name=Java           # → Hello, Java!
curl -X POST -d "ping" http://localhost:8080/echo    # → You sent: ping
curl http://localhost:8080/nope                      # → 404 Not Found
```

### Common Mistakes

- **Forgetting trailing slashes** — `/hello` and `/hello/` should match the same handler
- **Storing routes in a `HashMap`** — order matters for wildcard/param routes later. Use `List<Route>`
- **Not stripping query string before matching** — `/hello?name=x` is path `/hello`, not `/hello?name=x`

---

## Phase 4 — Middleware Pipeline

> **Goal:** Build a chain-of-responsibility system where requests pass through logging, auth, CORS, etc. before reaching the route handler.

### Concepts

- Middleware pattern — used in Express, Spring interceptors, servlet filters
- Chain of responsibility design pattern
- How `next()` works — each middleware decides to continue or short-circuit
- Request/response decoration

### Tasks

| #   | Task                        | Details                                                                                |
| --- | --------------------------- | -------------------------------------------------------------------------------------- |
| 4.1 | Create `Middleware` interface | `@FunctionalInterface`: `void handle(HttpRequest req, HttpResponse res, Runnable next)` |
| 4.2 | Create `MiddlewareChain`     | Takes `List<Middleware>` + final `RouteHandler`. Calling `next` advances to the next.  |
| 4.3 | Build `LoggingMiddleware`    | Prints `[timestamp] GET /hello → 200 (12ms)`                                          |
| 4.4 | Build `CorsMiddleware`       | Adds `Access-Control-Allow-Origin: *` header to every response                        |
| 4.5 | Build `AuthMiddleware`       | Checks `Authorization` header. If missing, return 401 and don't call `next`.           |
| 4.6 | Register in `App.java`       | `server.use(new LoggingMiddleware())` before routes                                    |

### Execution flow

```
Request arrives
  → LoggingMiddleware (start timer, call next)
    → CorsMiddleware (add header, call next)
      → AuthMiddleware (check auth, call next if OK)
        → RouteHandler (build response)
      ← AuthMiddleware
    ← CorsMiddleware
  ← LoggingMiddleware (log elapsed time)
Response sent
```

### Checkpoint

```bash
curl http://localhost:8080/admin                                       # → 401 Unauthorized
curl -H "Authorization: Bearer token123" http://localhost:8080/admin   # → 200 OK
```

Terminal shows formatted log lines for every request.

### Common Mistakes

- **Forgetting to call `next.run()`** — the request hangs forever (no response sent)
- **Calling `next.run()` twice** — handler executes twice, duplicating side effects
- **Building the chain with recursion** — hard to debug. Use an index-based approach: `chain[i].handle(req, res, () -> runNext(i + 1))`

---

## Phase 5 — Multi-Threading

> **Goal:** Handle multiple requests simultaneously instead of one-at-a-time.

### Concepts

- Why single-threaded servers block — while handling request A, request B waits
- Thread-per-request model (what Tomcat does by default)
- Thread pools and `ExecutorService` — why unlimited threads kill performance
- Thread safety — what happens when two threads touch the same data

### Tasks

| #   | Task                        | Details                                                                                            |
| --- | --------------------------- | -------------------------------------------------------------------------------------------------- |
| 5.1 | Observe the problem         | Add `Thread.sleep(5000)` in a handler. Open two tabs — second waits for first.                     |
| 5.2 | Thread-per-request (naive)  | Wrap `ConnectionHandler` in `new Thread(handler).start()`. Both tabs load simultaneously.          |
| 5.3 | Add `ThreadPoolManager`     | Replace raw threads with `Executors.newFixedThreadPool(10)`. Submit handlers to the pool.          |
| 5.4 | Make it configurable        | `new WireServer(8080, router).threads(20).start()`                                                 |
| 5.5 | Add graceful shutdown       | On `Ctrl+C` (shutdown hook), stop accepting, wait for in-flight requests, then exit.               |

### Thread pool sizing

| Workload  | Formula              | Why                                              |
| --------- | -------------------- | ------------------------------------------------ |
| CPU-bound | `cores`              | More threads just cause context-switch overhead   |
| I/O-bound | `cores * 2` to `* 10` | Threads spend time waiting, so more can overlap |

Start with a fixed pool of 10 — more than enough for learning.

### Checkpoint

Add a 3-second sleep to a handler. Open 5 browser tabs simultaneously. They should all complete in ~3 seconds (parallel), not ~15 seconds (serial).

### Common Mistakes

- **Creating threads in an unbounded loop** — under load you'll create thousands and run out of memory
- **Sharing mutable state between handlers** — e.g., a request counter giving wrong numbers
- **Unhandled exceptions in thread tasks** — silently kill the thread with no logging
- **Not shutting down `ExecutorService`** — JVM won't exit because non-daemon threads are alive

---

## Phase 6 — Error Handling & Polish

> **Goal:** Handle bad requests, handler exceptions, and malformed input without crashing the server.

### Concepts

- Defensive programming — never trust client input
- Custom exception hierarchies
- Global error handlers (like Spring's `@ExceptionHandler`)
- HTTP semantics: 400 vs 404 vs 500

### Tasks

| #   | Task                          | Details                                                                                      |
| --- | ----------------------------- | -------------------------------------------------------------------------------------------- |
| 6.1 | Create `HttpException`        | Extends `RuntimeException` with `int statusCode` and `String message`                        |
| 6.2 | Create `HttpStatus` enum      | `OK(200)`, `BAD_REQUEST(400)`, `NOT_FOUND(404)`, `INTERNAL_ERROR(500)`, etc.                 |
| 6.3 | Build `ErrorHandler`          | Wraps entire request pipeline in try-catch. Maps exceptions to HTTP responses.                |
| 6.4 | Handle malformed requests     | If `HttpParser` can't parse input, return `400 Bad Request` instead of crashing.             |
| 6.5 | Handle handler exceptions     | If a route handler throws, catch it and return 500 with a safe message.                      |
| 6.6 | Add `MimeType` utility        | Constants for `text/plain`, `text/html`, `application/json`. Set `Content-Type` accordingly. |

### Checkpoint

```bash
echo "GARBAGE" | nc localhost 8080           # → 400 Bad Request
curl http://localhost:8080/explode           # → 500 Internal Server Error (safe message)
curl http://localhost:8080/hello             # → 200 Hello, world! (still works)
```

### Common Mistakes

- **Returning stack traces in responses** — security vulnerability (leaks internal paths, class names)
- **Catching `Exception` and swallowing it** — always log server-side even if you hide it from the client
- **Not setting `Content-Length` on error responses** — some clients/proxies behave oddly
- **Letting one broken connection crash the server** — always catch at the connection handler level

---

## Phase 7 (Optional) — Extensions

Independent modules to tackle in any order once Phases 1–6 are solid.

### 7A — JSON Support

| Task                               | Details                                                              |
| ---------------------------------- | -------------------------------------------------------------------- |
| Build `JsonUtil.java`              | `toJson(Map)` → string, `fromJson(String)` → `Map<String, Object>`  |
| Manual parsing only                | No Gson, no Jackson. Parse `{"key": "value", "num": 42}` by hand.   |
| Add `req.json()` and `res.json()`  | Convenience methods that handle `Content-Type: application/json`     |

Start with flat key-value JSON only. Nested objects and arrays are significantly harder.

### 7B — Path Parameters

| Task                                   | Details                                                             |
| -------------------------------------- | ------------------------------------------------------------------- |
| Support `/users/:id` patterns          | `router.get("/users/:id", handler)`                                 |
| Extract into `req.getPathParam("id")`  | Match `/users/42` → `id = "42"`                                     |
| Handle conflicts                       | `/users/profile` (exact) should win over `/users/:id` (pattern)     |

### 7C — Static File Serving

| Task                                    | Details                                                             |
| --------------------------------------- | ------------------------------------------------------------------- |
| Serve files from a `public/` directory  | `GET /style.css` → reads `public/style.css`                         |
| Set correct `Content-Type`              | `.html` → `text/html`, `.css` → `text/css`, `.js` → `application/javascript` |
| Prevent path traversal                  | `GET /../../etc/passwd` must NOT work. Validate resolved path stays inside `public/`. |

### 7D — Basic Dependency Injection

| Task                               | Details                                                               |
| ---------------------------------- | --------------------------------------------------------------------- |
| Build a `Container` class          | `container.register(UserService.class, new UserService())`            |
| Resolve dependencies in handlers   | `container.resolve(UserService.class)`                                |
| Wire into the server               | `server.register(UserService.class, instance)`                        |

This teaches what Spring's IoC container does at its core — a `Map<Class, Object>`.

---

## Progression Summary

```
Phase 1  →  "I can make a computer talk HTTP"
Phase 2  →  "I understand the anatomy of an HTTP message"
Phase 3  →  "I can build a real routing system like Express/Spring"
Phase 4  →  "I understand how middleware chains work"
Phase 5  →  "I know why and how servers handle concurrent requests"
Phase 6  →  "I can build production-grade error handling"
Phase 7  →  "I can extend a framework with real features"
```

## How to Work Through This

1. **One phase at a time.** Don't read ahead and pre-build. The mistakes in Phase 1 teach you why Phase 2's design matters.
2. **Test with `curl -v`** (verbose). It shows the exact bytes sent and received.
3. **Print everything.** When stuck, print the raw bytes. HTTP bugs are almost always "I'm not sending what I think I am."
4. **Commit after each phase.** You'll want to look back at how the code evolved.
5. **Resist early refactoring.** Get it working first. Refactor once you understand why the better design is better.

## Running

```bash
# Compile
javac -d bin $(find src/main -name "*.java")

# Run
java -cp bin io.wirecore.App
```

Server starts on `http://localhost:8080`.
