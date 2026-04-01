package io.wirecore.routing;

import io.wirecore.http.HttpMethod;

import java.util.Set;

/**
 * Contract for HTTP routing: route registration and resolution.
 * The default implementation is {@link DefaultRouter}.
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
    RouteMatch resolveMatch(HttpMethod method, String rawPath);

    /** Methods that would match this path (for 405 + Allow header). */
    Set<HttpMethod> allowedMethods(String rawPath);
}
