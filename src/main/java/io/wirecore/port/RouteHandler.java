package io.wirecore.port;

import io.wirecore.model.HttpRequest;
import io.wirecore.model.HttpResponse;

@FunctionalInterface
public interface RouteHandler {
    void handle(HttpRequest req, HttpResponse res);
}
