package io.wirecore.routing;

import io.wirecore.http.HttpRequest;
import io.wirecore.http.HttpResponse;

@FunctionalInterface
public interface RouteHandler {
    void handle(HttpRequest req, HttpResponse res);
}
