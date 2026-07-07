package coordinator;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.ConsistentNodeHashService;

public class CoordinatorServer {

    private final ConsistentNodeHashService nodeHashService =
        new ConsistentNodeHashService();

    private static final Logger logger = LoggerFactory.getLogger(
        CoordinatorServer.class
    );

    private final Map<String, NodeHandle> nodes = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    public record NodeHandle(String id, int port, Process process) {}

    /**
     * Spawns {@code count} nodes, verifies they're healthy, registers a shutdown hook,
     * and blocks the calling thread to keep the coordinator alive.
     */
    public void run(int count, int startingPort) {
        for (int i = 1; i <= count; i++) {
            String id = "node-" + i;
            int port = startingPort + (i - 1);
            spawnNode(id, port);
        }

        waitUntilHealthy();

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownAll));

        logger.info("Coordinator running with {} node(s)", count);
        blockUntilKilled();
    }

    public NodeHandle spawnNode(String id, int port) {
        if (nodes.containsKey(id)) {
            throw new IllegalStateException("Node " + id + " already exists");
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                "jbang",
                "Node.java",
                "--port=" + port,
                "--id=" + id
            );
            pb.redirectErrorStream(true); // merge stderr into stdout

            Process process = pb.start();
            NodeHandle handle = new NodeHandle(id, port, process);
            nodes.put(id, handle);

            // Pipe the child process's output into our own logs, prefixed by node id.
            // Runs on a virtual thread so it doesn't cost a platform thread per node.
            Executors.newVirtualThreadPerTaskExecutor().submit(() -> {
                try (var reader = process.inputReader()) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.info("[{}] {}", id, line);
                    }
                } catch (IOException e) {
                    logger.warn(
                        "[{}] Output stream closed: {}",
                        id,
                        e.getMessage()
                    );
                }
            });

            logger.info(
                "Spawned node [{}] on port {} (pid={})",
                id,
                port,
                process.pid()
            );
            nodeHashService.addNode(id);
            return handle;
        } catch (IOException e) {
            throw new RuntimeException("Failed to spawn node " + id, e);
        }
    }

    public void stopNode(String id) {
        NodeHandle handle = nodes.remove(id);
        if (handle == null) {
            logger.warn("Tried to stop unknown node [{}]", id);
            return;
        }
        handle.process().destroy();
        nodeHashService.removeNode(id);
        logger.info("Stopped node [{}]", id);
    }

    public boolean isAlive(String id) {
        NodeHandle handle = nodes.get(id);
        if (handle == null) return false;
        return handle.process().isAlive();
    }

    public boolean healthCheck(String id) {
        NodeHandle handle = nodes.get(id);
        if (handle == null) return false;

        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(
                    URI.create("http://localhost:" + handle.port() + "/health")
                )
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();
            HttpResponse<String> res = httpClient.send(
                req,
                HttpResponse.BodyHandlers.ofString()
            );
            return res.statusCode() == 200;
        } catch (Exception e) {
            logger.warn("Health check failed for [{}]: {}", id, e.getMessage());
            return false;
        }
    }

    public NodeHandle routeFor(String key) {
        String nodeId = nodeHashService.routeFor(key);
        return nodes.get(nodeId);
    }

    public Map<String, NodeHandle> listNodes() {
        return Map.copyOf(nodes);
    }

    public void shutdownAll() {
        nodes.keySet().forEach(this::stopNode);
    }

    private void waitUntilHealthy() {
        try {
            Thread.sleep(3000); // give processes a moment to boot before checking
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        listNodes().forEach((id, handle) -> {
            boolean healthy = healthCheck(id);
            logger.info(
                "{} on port {} -> healthy={}",
                id,
                handle.port(),
                healthy
            );
        });
    }

    private void blockUntilKilled() {
        try {
            shutdownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
