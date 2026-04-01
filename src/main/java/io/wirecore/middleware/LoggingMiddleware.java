package io.wirecore.middleware;

import io.wirecore.http.HttpRequest;
import io.wirecore.http.HttpResponse;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logs every request with method, path, response status, and elapsed time.
 *
 * <p>Output format: {@code [2026-04-01T12:00:00Z] GET /hello → 200 (4ms)}
 *
 * <p>The status code is read after {@code next.run()} returns, so it reflects
 * whatever status the downstream middleware or route handler set.
 */
public final class LoggingMiddleware implements Middleware {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    @Override
    public void handle(HttpRequest req, HttpResponse res, Runnable next) {
        long start = System.currentTimeMillis();
        next.run();
        long elapsed = System.currentTimeMillis() - start;

        String timestamp = FORMATTER.format(ZonedDateTime.now(ZoneOffset.UTC));
        System.out.printf("[%s] %s %s → %d (%dms)%n",
                timestamp,
                req.method().name(),
                req.path(),
                res.statusCode(),
                elapsed);
    }
}
