package io.wirecore.routing;

import io.wirecore.port.RouteHandler;
import io.wirecore.model.HttpMethod;

import java.util.Objects;

public final class Route {
    private final HttpMethod method;
    private final PathTemplate template;
    private final RouteHandler handler;

    private Route(HttpMethod method, PathTemplate template, RouteHandler handler) {
        this.method = method;
        this.template = template;
        this.handler = handler;
    }

    public static Route of(HttpMethod method, String pathPattern, RouteHandler handler) {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(pathPattern, "pathPattern");
        Objects.requireNonNull(handler, "handler");
        return new Route(method, PathTemplate.of(pathPattern), handler);
    }

    public HttpMethod method() {
        return method;
    }

    public PathTemplate template() {
        return template;
    }

    public RouteHandler handler() {
        return handler;
    }
}

