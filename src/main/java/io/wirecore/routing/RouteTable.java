package io.wirecore.routing;

import io.wirecore.port.RouteHandler;
import io.wirecore.port.RouteResult;
import io.wirecore.model.HttpMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Route storage with deterministic precedence:
 * - exact routes first
 * - then parameterized routes in registration order
 */
public final class RouteTable {
    private final Map<HttpMethod, Map<String, RouteHandler>> exact = new HashMap<>();
    private final Map<HttpMethod, List<Route>> parameterized = new HashMap<>();

    public void add(Route route) {
        Objects.requireNonNull(route, "route");
        HttpMethod method = route.method();

        if (!route.template().isParameterized()) {
            exact.computeIfAbsent(method, __ -> new HashMap<>())
                    .put(route.template().pattern(), route.handler());
            return;
        }

        parameterized.computeIfAbsent(method, __ -> new ArrayList<>()).add(route);
    }

    public RouteResult resolve(HttpMethod method, String rawPath) {
        String path = PathNormalizer.normalize(rawPath);

        Map<String, RouteHandler> exactByPath = exact.get(method);
        if (exactByPath != null) {
            RouteHandler handler = exactByPath.get(path);
            if (handler != null) return new RouteResult(handler, Map.of());
        }

        List<Route> paramRoutes = parameterized.get(method);
        if (paramRoutes == null) return null;

        for (Route route : paramRoutes) {
            Map<String, String> params = route.template().match(path);
            if (params != null) return new RouteResult(route.handler(), params);
        }

        return null;
    }

    public Set<HttpMethod> allowedMethods(String rawPath) {
        String path = PathNormalizer.normalize(rawPath);
        Set<HttpMethod> allowed = new HashSet<>();

        for (Map.Entry<HttpMethod, Map<String, RouteHandler>> e : exact.entrySet()) {
            if (e.getValue().containsKey(path)) {
                allowed.add(e.getKey());
            }
        }

        for (Map.Entry<HttpMethod, List<Route>> e : parameterized.entrySet()) {
            HttpMethod method = e.getKey();
            if (allowed.contains(method)) continue;
            for (Route route : e.getValue()) {
                if (route.template().match(path) != null) {
                    allowed.add(method);
                    break;
                }
            }
        }

        return Set.copyOf(allowed);
    }
}

