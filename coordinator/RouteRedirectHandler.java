package coordinator;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import http.HttpResponseWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.ConsistentNodeHashService;

public class RouteRedirectHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(
        RouteRedirectHandler.class
    );
    private static final Gson GSON = new Gson();

    private final CoordinatorServer coordinator;
    private final ConsistentNodeHashService consistentNodeHashService;

    public RouteRedirectHandler(
        CoordinatorServer coordinator,
        ConsistentNodeHashService service
    ) {
        this.coordinator = coordinator;
        this.consistentNodeHashService = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String body;
        try (InputStream is = exchange.getRequestBody()) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        String key;
        try {
            JsonObject json = GSON.fromJson(body, JsonObject.class);
            if (json == null || !json.has("key")) {
                HttpResponseWriter.send(
                    exchange,
                    400,
                    "Missing 'key' in request body"
                );
                return;
            }
            key = json.get("key").getAsString();
        } catch (JsonSyntaxException e) {
            HttpResponseWriter.send(exchange, 400, "Malformed JSON body");
            return;
        }

        String nodeId = consistentNodeHashService.routeFor(key);
        CoordinatorServer.NodeHandle target = coordinator.getNode(nodeId);

        if (target == null) {
            logger.error(
                "Router picked node [{}] but it isn't registered",
                nodeId
            );
            HttpResponseWriter.send(exchange, 500, "Routing error");
            return;
        }

        URI redirectUri = URI.create("http://localhost:" + target.port() + "/");

        logger.info("Redirecting key={} -> {} ({})", key, nodeId, redirectUri);

        exchange.getResponseHeaders().set("Location", redirectUri.toString());
        exchange.sendResponseHeaders(307, -1); // preserves method + body, incl. GET-with-body
        exchange.close();
    }
}
