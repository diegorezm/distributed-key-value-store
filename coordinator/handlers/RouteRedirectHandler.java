package coordinator.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import coordinator.NodeHandle;
import coordinator.NodeProcessManager;
import coordinator.services.ConsistentNodeHashService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouteRedirectHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(RouteRedirectHandler.class);
    private static final Gson GSON = new Gson();

    private final NodeProcessManager processManager;
    private final ConsistentNodeHashService router;

    public RouteRedirectHandler(NodeProcessManager processManager, ConsistentNodeHashService router) {
        this.processManager = processManager;
        this.router = router;
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
                sendError(exchange, 400, "Missing 'key' in request body");
                return;
            }
            key = json.get("key").getAsString();
        } catch (JsonSyntaxException e) {
            sendError(exchange, 400, "Malformed JSON body");
            return;
        }

        String nodeId = router.routeFor(key);
        NodeHandle target = processManager.getNode(nodeId);

        if (target == null) {
            logger.error("Router picked node [{}] but it isn't registered", nodeId);
            sendError(exchange, 500, "Routing error");
            return;
        }

        URI redirectUri = URI.create("http://localhost:" + target.port() + "/");

        logger.info("Redirecting key={} -> {} ({})", key, nodeId, redirectUri);

        exchange.getResponseHeaders().set("Location", redirectUri.toString());
        exchange.sendResponseHeaders(307, -1);
        exchange.close();
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
