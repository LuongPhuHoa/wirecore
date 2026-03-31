package io.wirecore.abstraction;

import java.io.IOException;

/**
 * Contract for an HTTP server lifecycle (bind + accept loop).
 */
public interface HttpServer {
    void start() throws IOException;
}
