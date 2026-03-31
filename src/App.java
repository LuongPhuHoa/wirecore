import io.wirecore.abstraction.HttpServer;
import io.wirecore.abstraction.Router;
import io.wirecore.server.WireServer;
import io.wirecore.service.DefaultRouter;

public class App {
    public static void main(String[] args) throws Exception {
        Router router = new DefaultRouter();

        router.get("/", (req, res) -> res.status(200).body("Welcome to WireCore!"));

        router.get("/hello", (req, res) -> {
            String name = req.getQueryParam("name");
            res.status(200).body("Hello, " + (name != null ? name : "world") + "!");
        });

        router.post("/echo", (req, res) -> res.status(200).body("You sent: " + req.getBody()));

        HttpServer server = new WireServer(8080, router);
        server.start();
    }
}
