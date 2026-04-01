package io.wirecore.server;

import io.wirecore.http.HttpRequestParser;
import io.wirecore.middleware.Middleware;
import io.wirecore.routing.Router;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Accepts TCP connections and dispatches each to a {@link ConnectionHandler} (thread-per-connection).
 */
public final class WireServer implements HttpServer {
    private final int port;
    private final Router router;
    private final HttpRequestParser requestParser;
    private final List<Middleware> middlewares = new ArrayList<>();

    public WireServer(int port, Router router) {
        this(port, router, new HttpParser());
    }

    public WireServer(int port, Router router, HttpRequestParser requestParser) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }
        this.port = port;
        this.router = Objects.requireNonNull(router, "router");
        this.requestParser = Objects.requireNonNull(requestParser, "requestParser");
    }

    /**
     * Registers a global middleware that runs for every request, in registration order.
     * Must be called before {@link #start()}.
     */
    public WireServer use(Middleware middleware) {
        middlewares.add(Objects.requireNonNull(middleware, "middleware"));
        return this;
    }

    @Override
    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("WireCore listening on http://localhost:" + port);
            while (!Thread.currentThread().isInterrupted()) {
                Socket client = serverSocket.accept();
                Thread worker = new Thread(
                        new ConnectionHandler(client, router, requestParser, List.copyOf(middlewares)),
                        "wirecore-conn"
                );
                worker.start();
            }
        }
    }
}
