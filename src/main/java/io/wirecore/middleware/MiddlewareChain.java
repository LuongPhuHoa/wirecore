package io.wirecore.middleware;

import io.wirecore.http.HttpRequest;
import io.wirecore.http.HttpResponse;
import io.wirecore.routing.RouteHandler;

import java.util.List;
import java.util.Objects;

/**
 * Executes a list of {@link Middleware} steps followed by a terminal {@link RouteHandler}.
 *
 * <p>Uses an index-based approach so the call stack stays flat and predictable.
 * Each middleware receives a {@code next} lambda that advances the index by one.
 *
 * <p>Example flow with two middlewares:
 * <pre>
 *   chain[0].handle(req, res, () -> runAt(1))
 *     chain[1].handle(req, res, () -> runAt(2))
 *       terminal.handle(req, res)   // index == size
 * </pre>
 */
public final class MiddlewareChain {
    private final List<Middleware> middlewares;
    private final RouteHandler terminal;

    public MiddlewareChain(List<Middleware> middlewares, RouteHandler terminal) {
        this.middlewares = List.copyOf(Objects.requireNonNull(middlewares, "middlewares"));
        this.terminal = Objects.requireNonNull(terminal, "terminal");
    }

    /**
     * Starts executing the chain from the first middleware.
     */
    public void execute(HttpRequest req, HttpResponse res) {
        runAt(req, res, 0);
    }

    private void runAt(HttpRequest req, HttpResponse res, int index) {
        if (index < middlewares.size()) {
            middlewares.get(index).handle(req, res, () -> runAt(req, res, index + 1));
        } else {
            terminal.handle(req, res);
        }
    }
}
