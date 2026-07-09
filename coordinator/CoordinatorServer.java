package coordinator;

import com.sun.net.httpserver.HttpServer;

import coordinator.handlers.RouteRedirectHandler;
import coordinator.services.ConsistentNodeHashService;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoordinatorServer {
    private static final Logger logger = LoggerFactory.getLogger(CoordinatorServer.class);

    private final NodeProcessManager processManager = new NodeProcessManager();
    private final ConsistentNodeHashService nodeHashService = new ConsistentNodeHashService();
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    public record NodeInfo(String id, int port) {}

    public void run(int count, int startingPort, int coordinatorPort, int replicationFactor) throws Exception {
        List<NodeInfo> allNodes = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            allNodes.add(new NodeInfo("node-" + i, startingPort + i - 1));
        }

        for (NodeInfo node : allNodes) {
            nodeHashService.addNode(node.id());
        }

        for (NodeInfo self : allNodes) {
              List<String> replicaIds = replicasFor(self.id(), replicationFactor);
              String peerArg = replicaIds.stream()
                  .map(id -> id + "=http://localhost:" + portFor(allNodes, id))
                  .collect(Collectors.joining(","));

              processManager.spawnNode(self.id(), self.port(), peerArg);
        }

          waitUntilHealthy();
        startHttpServer(coordinatorPort);

        Runtime.getRuntime().addShutdownHook(new Thread(processManager::shutdownAll));

        logger.info("Coordinator running with {} node(s), replication factor {}, on port {}",
              count, replicationFactor, coordinatorPort);
        shutdownLatch.await();
    }

    private void waitUntilHealthy() throws InterruptedException {
        Thread.sleep(2000);
        processManager.listNodes().forEach((id, handle) -> {
            boolean healthy = processManager.healthCheck(id);
            logger.info("{} on port {} -> healthy={}", id, handle.port(), healthy);
        });
    }

    private void startHttpServer(int port) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RouteRedirectHandler(processManager, nodeHashService));
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        logger.info("Coordinator HTTP server listening on port {}", port);
    }

    /**
     * Computes which nodes should be replicas for {@code nodeId}, using the node's own
     * ring position as the starting point and walking clockwise to its neighbors.
     */
    private List<String> replicasFor(String nodeId, int replicationFactor) {
        // Pull one extra candidate as a buffer in case nodeId happens to appear in the result;
        // filtering it out and capping afterward guarantees exactly replicationFactor entries either way.
        List<String> ring = nodeHashService.routeFor(nodeId, replicationFactor + 1);
        return ring.stream()
            .filter(id -> !id.equals(nodeId))
            .limit(replicationFactor)
            .toList();
    }

    private int portFor(List<NodeInfo> allNodes, String id) {
        return allNodes.stream()
            .filter(n -> n.id().equals(id))
            .findFirst()
            .map(NodeInfo::port)
            .orElseThrow(() -> new IllegalStateException("Unknown node id: " + id));
    }
}
