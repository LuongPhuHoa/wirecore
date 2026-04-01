package io.wirecore.middleware;

import io.wirecore.http.HttpRequest;
import io.wirecore.http.HttpResponse;

/**
 * Enforces the presence of an {@code Authorization} header on every request.
 *
 * <p>Returns {@code 401 Unauthorized} and short-circuits the chain when the header is absent.
 * The response body is a JSON error object consistent with the rest of the error format.
 *
 * <p>This is intentionally simple — it only checks that the header exists.
 * Token validation belongs in a subclass or a dedicated authentication service.
 */
public final class AuthMiddleware implements Middleware {
    @Override
    public void handle(HttpRequest req, HttpResponse res, Runnable next) {
        String auth = req.header("authorization");
        if (auth == null || auth.isBlank()) {
            res.status(401)
               .header("Content-Type", "application/json; charset=utf-8")
               .body("{\"status\":401,\"error\":\"unauthorized\",\"message\":\"Authorization header is required\"}");
            return;
        }
        next.run();
    }
}
