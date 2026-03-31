package io.wirecore.abstraction;

import io.wirecore.model.HttpMethod;

import java.util.Set;

/**
 * Contract for HTTP routing: registration and resolution.
 * Implementations live in {@link io.wirecore.service.DefaultRouter}.
 */
public interface Router {
    Router addRoute(String method, String pathPattern, RouteHandler handler);

    Router addRoute(HttpMethod method, String pathPattern, RouteHandler handler);

    Router get(String pathPattern, RouteHandler handler);

    Router post(String pathPattern, RouteHandler handler);

    Router put(String pathPattern, RouteHandler handler);

    Router patch(String pathPattern, RouteHandler handler);

    Router delete(String pathPattern, RouteHandler handler);

    /** Returns null when not found. */
    RouteHandler resolve(HttpMethod method, String rawPath);

    /** Returns null when not found. */
    RouteHandler resolve(String method, String rawPath);

    /** Returns null when not found. */
    RouteResult resolveResult(HttpMethod method, String rawPath);

    /** Methods that would match this path (for 405 + Allow). */
    Set<HttpMethod> allowedMethods(String rawPath);
}
