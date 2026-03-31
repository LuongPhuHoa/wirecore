package io.wirecore.port;

import java.util.Map;
import java.util.Objects;

/**
 * Result of matching a route: handler plus extracted path parameters.
 */
public record RouteResult(RouteHandler handler, Map<String, String> pathParams) {
    public RouteResult {
        Objects.requireNonNull(handler, "handler");
        pathParams = pathParams == null ? Map.of() : Map.copyOf(pathParams);
    }
}
