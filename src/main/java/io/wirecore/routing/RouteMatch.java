package io.wirecore.routing;

import java.util.Map;
import java.util.Objects;

/**
 * The result of a successful route resolution: the matched handler plus any extracted path parameters.
 */
public record RouteMatch(RouteHandler handler, Map<String, String> pathParams) {
    public RouteMatch {
        Objects.requireNonNull(handler, "handler");
        pathParams = pathParams == null ? Map.of() : Map.copyOf(pathParams);
    }
}
