package coordinator.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import coordinator.NodeHandle;
import coordinator.NodeHealthMonitor;
import coordinator.NodeProcessManager;
import coordinator.services.ConsistentNodeHashService;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shared.http.HttpResponseWriter;

public class RouteRedirectHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(
        RouteRedirectHandler.class
    );
    private static final Gson GSON = new Gson();

    private final NodeProcessManager processManager;
    private final ConsistentNodeHashService router;
    private final NodeHealthMonitor healthMonitor;
    private int replicationFactor;

    public RouteRedirectHandler(
        NodeProcessManager processManager,
        ConsistentNodeHashService router,
        NodeHealthMonitor nodeHealthMonitor,
        int replicationFactor
    ) {
        this.processManager = processManager;
        this.router = router;
        this.healthMonitor = nodeHealthMonitor;
        this.replicationFactor = replicationFactor;
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

        List<String> candidates = router.routeFor(key, replicationFactor + 1);

        String nodeId = candidates
            .stream()
            .filter(healthMonitor::isHealthy)
            .findFirst()
            .orElse(null);

        if (nodeId == null) {
            logger.error(
                "No healthy node found for key={} among candidates={}",
                key,
                candidates
            );
            HttpResponseWriter.send(
                exchange,
                503,
                "No healthy node available for this key"
            );
            return;
        }

        NodeHandle target = processManager.getNode(nodeId);

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
        exchange.sendResponseHeaders(307, -1);
        exchange.close();
    }
}
