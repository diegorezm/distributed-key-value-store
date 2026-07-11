package src.main.java.kvcluster.coordinator.infra;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import src.main.java.kvcluster.coordinator.domain.HealthChecker;
import src.main.java.kvcluster.coordinator.domain.NodeRegistry;
import src.main.java.kvcluster.coordinator.domain.model.NodeHandle;

/**
 * ADAPTER — implements HealthChecker by sending an HTTP GET /health
 * to the node's port as looked up from NodeRegistry.
 */
public class NodeHealthProbe implements HealthChecker {

    private static final Logger logger = LoggerFactory.getLogger(NodeHealthProbe.class);

    private final NodeRegistry registry;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public NodeHealthProbe(NodeRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean healthCheck(String nodeId) {
        NodeHandle handle = registry.getNode(nodeId);
        if (handle == null) return false;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + handle.port() + "/health"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            return res.statusCode() == 200;
        } catch (Exception e) {
            logger.warn("Health check failed for [{}]: {}", nodeId, e.getMessage());
            return false;
        }
    }
}
