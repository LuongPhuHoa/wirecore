package io.wirecore.routing;

import io.wirecore.http.HttpMethod;

import java.util.Objects;
import java.util.Set;

/**
 * Default {@link Router} implementation backed by {@link RouteRegistry}.
 */
public final class DefaultRouter implements Router {
    private final RouteRegistry registry = new RouteRegistry();

    @Override
    public DefaultRouter addRoute(String method, String pathPattern, RouteHandler handler) {
        HttpMethod m = HttpMethod.parse(method);
        if (m == null) throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        return addRoute(m, pathPattern, handler);
    }

    @Override
    public DefaultRouter addRoute(HttpMethod method, String pathPattern, RouteHandler handler) {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(pathPattern, "pathPattern");
        Objects.requireNonNull(handler, "handler");
        registry.register(Route.of(method, pathPattern, handler));
        return this;
    }

    @Override
    public DefaultRouter get(String pathPattern, RouteHandler handler) {
        return addRoute(HttpMethod.GET, pathPattern, handler);
    }

    @Override
    public DefaultRouter post(String pathPattern, RouteHandler handler) {
        return addRoute(HttpMethod.POST, pathPattern, handler);
    }

    @Override
    public DefaultRouter put(String pathPattern, RouteHandler handler) {
        return addRoute(HttpMethod.PUT, pathPattern, handler);
    }

    @Override
    public DefaultRouter patch(String pathPattern, RouteHandler handler) {
        return addRoute(HttpMethod.PATCH, pathPattern, handler);
    }

    @Override
    public DefaultRouter delete(String pathPattern, RouteHandler handler) {
        return addRoute(HttpMethod.DELETE, pathPattern, handler);
    }

    @Override
    public RouteHandler resolve(HttpMethod method, String rawPath) {
        RouteMatch match = resolveMatch(method, rawPath);
        return match == null ? null : match.handler();
    }

    @Override
    public RouteHandler resolve(String method, String rawPath) {
        HttpMethod m = HttpMethod.parse(method);
        if (m == null) return null;
        return resolve(m, rawPath);
    }

    @Override
    public RouteMatch resolveMatch(HttpMethod method, String rawPath) {
        if (method == null) return null;
        return registry.resolve(method, rawPath);
    }

    @Override
    public Set<HttpMethod> allowedMethods(String rawPath) {
        return registry.allowedMethods(rawPath);
    }
}
