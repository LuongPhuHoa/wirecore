package io.wirecore.middleware;

import io.wirecore.http.HttpRequest;
import io.wirecore.http.HttpResponse;

/**
 * A single step in the middleware pipeline.
 *
 * <p>Implementations receive the request, the response, and a {@code next} callback.
 * Call {@code next.run()} to advance to the next middleware (or the terminal route handler).
 * Omit the call to short-circuit the chain — the response should be populated before returning.
 */
@FunctionalInterface
public interface Middleware {
    void handle(HttpRequest req, HttpResponse res, Runnable next);
}
