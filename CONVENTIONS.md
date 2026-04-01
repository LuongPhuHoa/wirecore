# WireCore Conventions

Naming and structural conventions for every contributor and every phase of the project.
This document governs the whole codebase — HTTP, routing, middleware, DI, and everything that comes after.

---

## 1. Package Philosophy

Packages are organized by **feature**, not by architectural layer.

Do not use layer names like `model`, `service`, `repository`, `port`, or `adapter` as top-level packages.
Each package owns everything related to a single concern: its public contracts (interfaces), its data types, and its implementations.

```
io.wirecore/
├── http/          # HTTP protocol — request, response, method, parsing
├── routing/       # Route registration, path matching, handler dispatch
├── server/        # TCP lifecycle, connection handling, the HttpServer contract
├── middleware/    # (Phase 4) Middleware chain and built-in middlewares
├── context/       # (Phase 7D) Dependency injection container
├── error/         # (Phase 6) Exception types and global error handler
├── util/          # (Phase 6–7) HttpStatus, MimeType, JsonUtil
└── thread/        # (Phase 5) ThreadPoolManager
```

**Rule:** if you can not describe a package in three words or fewer, it is too broad or too abstract.

---

## 2. Naming Rules

### Classes

| Kind | Convention | Examples |
|---|---|---|
| Class | `PascalCase` | `HttpRequest`, `WireServer`, `RouteRegistry` |
| Interface | `PascalCase`, no prefix/suffix | `Router`, `RouteHandler`, `HttpServer` |
| Default implementation | `Default` prefix | `DefaultRouter` |
| Branded/named implementation | Framework name prefix | `WireServer`, `HttpParser` |
| Enum | `PascalCase` | `HttpMethod`, `HttpStatus` |
| Record | `PascalCase` | `RouteMatch` |
| Exception | `...Exception` suffix | `HttpException` |

Do **not** use `I` prefix on interfaces (`IRouter` is wrong). Do **not** use `Impl` suffix on implementations (`RouterImpl` is wrong).

### Methods

| Kind | Convention | Examples |
|---|---|---|
| General | `camelCase` | `resolveMatch()`, `addRoute()` |
| Boolean getter | `is...` or `has...` | `isParameterized()`, `hasBody()` |
| Factory | `of(...)` | `Route.of(...)`, `PathPattern.of(...)` |
| Conversion | `to...` | `toBytes()`, `toString()` |
| Fluent builder step | verb or noun, returns `this` | `status(200)`, `header(...)`, `body(...)` |

### Fields and Variables

`camelCase` always. No Hungarian notation. No single-letter names except loop counters (`i`, `j`) and short lambdas.

### Constants

`UPPER_SNAKE_CASE` for `static final` primitives and strings.

```java
private static final int MAX_BODY_SIZE = 10 * 1024 * 1024;
private static final Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;
```

### Packages

All lowercase, single words. No underscores.

```
io.wirecore.http       ✓
io.wirecore.routing    ✓
io.wirecore.httpUtils  ✗  (camelCase not allowed)
io.wirecore.http_util  ✗  (underscore not allowed)
```

---

## 3. Interface vs. Implementation

Every extensible contract is an **interface**. The default bundled implementation uses the `Default` prefix.

```
Router              ← interface in io.wirecore.routing
DefaultRouter       ← implementation in io.wirecore.routing
```

```
HttpServer          ← interface in io.wirecore.server
WireServer          ← branded implementation (the framework's own server)
```

```
HttpRequestParser   ← interface in io.wirecore.http
HttpParser          ← concrete implementation in io.wirecore.server
```

Place the interface and its default implementation in the **same package**. Do not create a separate `impl` sub-package unless the package grows beyond eight or nine files.

---

## 4. File Layout Within a Class

Follow this order inside every class file:

1. `static final` constants
2. Instance fields
3. Constructors (no-arg first, then most-specific)
4. Public interface methods (in the same order as declared in the interface)
5. Private helper methods
6. Private static methods
7. Private nested / inner classes

---

## 5. Code Style Rules

- **No `null` return from public methods** when an `Optional` or exception is more appropriate — but within this project, `null` is accepted for resolution results (`resolveMatch` returns `null` when no route matches) because it is documented and checked immediately by the caller.
- **Immutable data objects.** `HttpRequest` fields are set once at construction. Defensive copies are made on input.
- **Fluent builders return `this`.** `HttpResponse` methods chain.
- **Factory methods over constructors** when construction requires non-trivial logic. Use `Foo.of(...)`.
- **`Objects.requireNonNull`** at every public method boundary for required parameters.
- **No checked exceptions in interfaces** except `IOException` for I/O contracts (`HttpRequestParser.parse`, `HttpServer.start`). Handler logic does not throw checked exceptions.
- **No framework dependencies.** Core Java only: `java.net`, `java.io`, `java.util`, `java.util.concurrent`, `java.time`.

---

## 6. Comments and Javadoc

Write a Javadoc comment on every **public interface** and every **public class** that is not self-evident from its name.

Do **not** write comments that restate the code:

```java
// Bad: narrates what is obvious
// Increment the counter
count++;

// Good: explains non-obvious intent
// Content-Length is omitted for 1xx/204/304 per RFC 7230 §3.3.2
```

Use `{@link ClassName}` in Javadoc to cross-reference related types.

---

## 7. Adding a New Feature

When adding a new phase or capability, follow this checklist:

1. **Create a new package** under `io.wirecore.<feature>` (see expansion slots above).
2. **Define the public contract first** as an interface.
3. **Write the default implementation** in the same package, prefixed with `Default` or use a descriptive name if there will only ever be one implementation.
4. **Update `App.java`** only to wire the feature in — no business logic lives in `App`.
5. **Update this document** with the new package in the expansion table above and any new naming rules.

---

## 8. Anti-Patterns to Avoid

| Anti-pattern | Why it is wrong | What to do instead |
|---|---|---|
| `model`, `service`, `port` top-level packages | Layer names say nothing about what the code does | Use feature names: `http`, `routing`, `server` |
| `RouterImpl`, `IRouter` | Redundant suffixes/prefixes pollute names | `DefaultRouter` for the default; the interface is just `Router` |
| Utility classes named `Utils` or `Helper` | Vague catch-all | Name by what it does: `PathNormalizer`, `JsonSerializer` |
| Catching `Exception` silently | Hides bugs | Log server-side; return a safe error response client-side |
| Sharing mutable state across handlers | Race conditions | Keep handler state in local variables or immutable objects |
| Returning raw stack traces in HTTP responses | Security leak | Map to a generic 500 message; log the full trace server-side |
| Unbounded thread creation | OOM under load | Use a fixed `ExecutorService` (`ThreadPoolManager`, Phase 5) |
| Reading request body with `BufferedReader.readLine()` | Blocks on keep-alive connections | Read exactly `Content-Length` bytes from the raw `InputStream` |
