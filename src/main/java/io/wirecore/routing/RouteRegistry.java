package io.wirecore.routing;

import io.wirecore.http.HttpMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Stores registered routes with deterministic resolution precedence:
 * exact paths are matched before parameterized patterns, and parameterized
 * patterns are matched in registration order.
 */
public final class RouteRegistry {
    private final Map<HttpMethod, Map<String, RouteHandler>> exact = new HashMap<>();
    private final Map<HttpMethod, List<Route>> parameterized = new HashMap<>();

    public void register(Route route) {
        Objects.requireNonNull(route, "route");
        HttpMethod method = route.method();

        if (!route.pattern().isParameterized()) {
            exact.computeIfAbsent(method, __ -> new HashMap<>())
                    .put(route.pattern().raw(), route.handler());
            return;
        }

        parameterized.computeIfAbsent(method, __ -> new ArrayList<>()).add(route);
    }

    public RouteMatch resolve(HttpMethod method, String rawPath) {
        String path = PathNormalizer.normalize(rawPath);

        Map<String, RouteHandler> exactByPath = exact.get(method);
        if (exactByPath != null) {
            RouteHandler handler = exactByPath.get(path);
            if (handler != null) return new RouteMatch(handler, Map.of());
        }

        List<Route> paramRoutes = parameterized.get(method);
        if (paramRoutes == null) return null;

        for (Route route : paramRoutes) {
            Map<String, String> params = route.pattern().match(path);
            if (params != null) return new RouteMatch(route.handler(), params);
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
                if (route.pattern().match(path) != null) {
                    allowed.add(method);
                    break;
                }
            }
        }

        return Set.copyOf(allowed);
    }
}
