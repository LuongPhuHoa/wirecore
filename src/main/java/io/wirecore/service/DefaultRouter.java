package io.wirecore.service;

import io.wirecore.port.RouteHandler;
import io.wirecore.port.RouteResult;
import io.wirecore.port.Router;
import io.wirecore.model.HttpMethod;
import io.wirecore.routing.Route;
import io.wirecore.routing.RouteTable;

import java.util.Objects;
import java.util.Set;

/**
 * Default {@link Router} implementation backed by {@link RouteTable}.
 */
public final class DefaultRouter implements Router {
    private final RouteTable table = new RouteTable();

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
        table.add(Route.of(method, pathPattern, handler));
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
        RouteResult r = resolveResult(method, rawPath);
        return r == null ? null : r.handler();
    }

    @Override
    public RouteHandler resolve(String method, String rawPath) {
        HttpMethod m = HttpMethod.parse(method);
        if (m == null) return null;
        return resolve(m, rawPath);
    }

    @Override
    public RouteResult resolveResult(HttpMethod method, String rawPath) {
        if (method == null) return null;
        return table.resolve(method, rawPath);
    }

    @Override
    public Set<HttpMethod> allowedMethods(String rawPath) {
        return table.allowedMethods(rawPath);
    }
}
