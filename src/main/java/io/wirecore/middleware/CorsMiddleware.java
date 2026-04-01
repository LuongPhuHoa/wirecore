package io.wirecore.middleware;

import io.wirecore.http.HttpMethod;
import io.wirecore.http.HttpRequest;
import io.wirecore.http.HttpResponse;

/**
 * Adds CORS headers to every response and handles OPTIONS preflight requests.
 *
 * <p>Preflight ({@code OPTIONS}) requests are answered immediately with {@code 204 No Content}
 * without advancing the chain, since there is no body to produce.
 */
public final class CorsMiddleware implements Middleware {
    private final String allowedOrigin;

    public CorsMiddleware() {
        this("*");
    }

    public CorsMiddleware(String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }

    @Override
    public void handle(HttpRequest req, HttpResponse res, Runnable next) {
        res.header("Access-Control-Allow-Origin", allowedOrigin);
        res.header("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        res.header("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if (req.method() == HttpMethod.OPTIONS) {
            res.status(204);
            return;
        }

        next.run();
    }
}
