package src.main.java.kvcluster.coordinator.transport;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import src.main.java.kvcluster.coordinator.NodeHealthMonitor;
import src.main.java.kvcluster.coordinator.domain.NodeRegistry;
import src.main.java.kvcluster.coordinator.domain.NodeRouter;
import src.main.java.kvcluster.coordinator.domain.model.NodeHandle;
import src.main.java.kvcluster.shared.http.HttpResponseWriter;

/**
 * TRANSPORT — reads the key from the request, picks a healthy node via the ring,
 * and issues a 307 redirect.
 * Depends only on domain ports.
 */
public class RouteRedirectHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(RouteRedirectHandler.class);
    private static final Gson GSON = new Gson();

    private final NodeRouter router;
    private final NodeRegistry registry;
    private final NodeHealthMonitor healthMonitor;
    private final int replicationFactor;

    public RouteRedirectHandler(
        NodeRouter router,
        NodeRegistry registry,
        NodeHealthMonitor healthMonitor,
        int replicationFactor
    ) {
        this.router = router;
        this.registry = registry;
        this.healthMonitor = healthMonitor;
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
                HttpResponseWriter.send(exchange, 400, "Missing 'key' in request body");
                return;
            }
            key = json.get("key").getAsString();
        } catch (JsonSyntaxException e) {
            HttpResponseWriter.send(exchange, 400, "Malformed JSON body");
            return;
        }

        List<String> candidates = router.routeFor(key, replicationFactor + 1);

        String nodeId = candidates.stream()
            .filter(healthMonitor::isHealthy)
            .findFirst()
            .orElse(null);

        if (nodeId == null) {
            logger.error("No healthy node found for key={} among candidates={}", key, candidates);
            HttpResponseWriter.send(exchange, 503, "No healthy node available for this key");
            return;
        }

        NodeHandle target = registry.getNode(nodeId);
        if (target == null) {
            logger.error("Router picked node [{}] but it isn't registered", nodeId);
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
