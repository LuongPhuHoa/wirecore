package io.wirecore.server;

import io.wirecore.abstraction.HttpRequestParser;
import io.wirecore.abstraction.HttpServer;
import io.wirecore.abstraction.Router;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;

/**
 * Accepts TCP connections and dispatches each to a {@link ConnectionHandler} (thread-per-connection).
 */
public final class WireServer implements HttpServer {
    private final int port;
    private final Router router;
    private final HttpRequestParser requestParser;

    public WireServer(int port, Router router) {
        this(port, router, new DefaultHttpRequestParser());
    }

    public WireServer(int port, Router router, HttpRequestParser requestParser) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }
        this.port = port;
        this.router = Objects.requireNonNull(router, "router");
        this.requestParser = Objects.requireNonNull(requestParser, "requestParser");
    }

    @Override
    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("WireCore listening on http://localhost:" + port);
            while (!Thread.currentThread().isInterrupted()) {
                Socket client = serverSocket.accept();
                Thread worker = new Thread(new ConnectionHandler(client, router, requestParser), "wirecore-conn");
                worker.start();
            }
        }
    }
}
