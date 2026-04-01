package io.wirecore.routing;

import io.wirecore.http.HttpMethod;

import java.util.Objects;

public final class Route {
    private final HttpMethod method;
    private final PathPattern pattern;
    private final RouteHandler handler;

    private Route(HttpMethod method, PathPattern pattern, RouteHandler handler) {
        this.method = method;
        this.pattern = pattern;
        this.handler = handler;
    }

    public static Route of(HttpMethod method, String pathPattern, RouteHandler handler) {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(pathPattern, "pathPattern");
        Objects.requireNonNull(handler, "handler");
        return new Route(method, PathPattern.of(pathPattern), handler);
    }

    public HttpMethod method() {
        return method;
    }

    public PathPattern pattern() {
        return pattern;
    }

    public RouteHandler handler() {
        return handler;
    }
}
