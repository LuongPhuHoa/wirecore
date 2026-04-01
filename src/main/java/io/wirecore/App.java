package io.wirecore;

import io.wirecore.middleware.AuthMiddleware;
import io.wirecore.middleware.CorsMiddleware;
import io.wirecore.middleware.LoggingMiddleware;
import io.wirecore.routing.DefaultRouter;
import io.wirecore.routing.Router;
import io.wirecore.server.WireServer;

public class App {
    public static void main(String[] args) throws Exception {
        Router router = new DefaultRouter();

        router.get("/", (req, res) ->
                res.status(200).body("Welcome to WireCore!"));

        router.get("/hello", (req, res) -> {
            String name = req.queryParam("name");
            res.status(200).body("Hello, " + (name != null ? name : "world") + "!");
        });

        router.post("/echo", (req, res) ->
                res.status(200).body("You sent: " + req.body()));

        router.get("/admin", (req, res) ->
                res.status(200).body("Welcome to the admin panel!"));

        WireServer server = new WireServer(8080, router);

        server.use(new LoggingMiddleware());
        server.use(new CorsMiddleware());

        server.start();
    }
}
