package src.test.java.kvcluster.integration;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import src.main.java.kvcluster.coordinator.CoordinatorServer;
import src.main.java.kvcluster.shared.models.ListNodeResponse;
import src.main.java.kvcluster.shared.models.NodeStatus;

public class TestCluster {

    private static final Gson GSON = new Gson();

    private final CoordinatorServer coordinator;
    private final int coordinatorPort;
    private final HttpClient client = HttpClient.newHttpClient();

    private TestCluster(CoordinatorServer coordinator, int coordinatorPort) {
        this.coordinator = coordinator;
        this.coordinatorPort = coordinatorPort;
    }

    public static TestCluster start(
        int nodeCount,
        int startingPort,
        int coordinatorPort,
        int replicationFactor
    ) throws Exception {
        CoordinatorServer coordinator = new CoordinatorServer(
            Path.of("build/node.jar")
        );

        Thread thread = new Thread(() -> {
            try {
                coordinator.run(
                    nodeCount,
                    startingPort,
                    coordinatorPort,
                    replicationFactor
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        thread.setDaemon(true);
        thread.start();

        TestCluster cluster = new TestCluster(coordinator, coordinatorPort);
        cluster.awaitReady(nodeCount);
        return cluster;
    }

    public void stop() {
        coordinator.shutdown();
    }

    public int coordinatorPort() {
        return coordinatorPort;
    }

    public HttpResponse<String> request(String method, String jsonBody)
        throws IOException, InterruptedException {
        var initial = client.send(
            HttpRequest.newBuilder(
                URI.create("http://localhost:" + coordinatorPort + "/")
            )
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(jsonBody))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );

        if (initial.statusCode() == 307) {
            String location = initial
                .headers()
                .firstValue("Location")
                .orElseThrow();
            return client.send(
                HttpRequest.newBuilder(URI.create(location))
                    .header("Content-Type", "application/json")
                    .method(
                        method,
                        HttpRequest.BodyPublishers.ofString(jsonBody)
                    )
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
        }
        return initial;
    }

    public HttpResponse<String> requestDirect(
        int port,
        String method,
        String url
    ) throws IOException, InterruptedException {
        return client.send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + url))
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    public HttpResponse<String> requestDirect(
        int port,
        String method,
        String path,
        String jsonBody
    ) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(
            URI.create("http://localhost:" + port + path)
        );
        builder
            .header("Content-Type", "application/json")
            .method(method, HttpRequest.BodyPublishers.ofString(jsonBody));
        return client.send(
            builder.build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    public ListNodeResponse listNodes()
        throws IOException, InterruptedException {
        var response = client.send(
            HttpRequest.newBuilder(
                URI.create("http://localhost:" + coordinatorPort + "/nodes")
            )
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        return GSON.fromJson(response.body(), ListNodeResponse.class);
    }

    private void awaitReady(int expectedNodeCount) throws Exception {
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            try {
                ListNodeResponse result = listNodes();
                long healthyCount = result
                    .nodes()
                    .stream()
                    .filter(NodeStatus::healthy)
                    .count();
                if (healthyCount >= expectedNodeCount) {
                    return;
                }
            } catch (IOException | InterruptedException ignored) {}
            Thread.sleep(300);
        }
        throw new IllegalStateException(
            "Cluster did not become healthy within timeout"
        );
    }

    public void killNodeProcess(String nodeId) {
        var handle = coordinator.getNodesHandle().get(nodeId);
        if (handle == null) {
            throw new IllegalArgumentException("No such node: " + nodeId);
        }
        handle.process().destroyForcibly();
    }

    public void awaitNodeMarkedDown(String nodeId)
        throws IOException, InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            var result = listNodes();
            boolean isDown = result
                .nodes()
                .stream()
                .filter(n -> n.id().equals(nodeId))
                .findFirst()
                .map(n -> !n.healthy())
                .orElse(false);

            if (isDown) {
                return;
            }
            Thread.sleep(300);
        }
        throw new IllegalStateException(
            "Node " + nodeId + " was not marked down within timeout"
        );
    }
}
