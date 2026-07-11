package src.main.java.kvcluster.coordinator;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import src.main.java.kvcluster.coordinator.domain.HealthChecker;
import src.main.java.kvcluster.coordinator.domain.NodeRouter;
import src.main.java.kvcluster.coordinator.handlers.ListNodesHandler;
import src.main.java.kvcluster.coordinator.handlers.RouteRedirectHandler;
import src.main.java.kvcluster.coordinator.services.ConsistentNodeHashService;

/**
 * Composition root — constructs all concretions and wires them together.
 * Nothing else in the coordinator package does `new` on a dependency.
 */
public class CoordinatorServer {

    private static final Logger logger = LoggerFactory.getLogger(CoordinatorServer.class);

    private final NodeProcessManager processManager;
    private final NodeRouter router;
    private final HealthChecker healthChecker;
    private final NodeHealthMonitor healthMonitor;
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    public CoordinatorServer(Path nodeJarPath) {
        this.processManager = new NodeProcessManager(nodeJarPath);
        this.router = new ConsistentNodeHashService();
        this.healthChecker = new NodeHealthProbe(processManager);
        this.healthMonitor = new NodeHealthMonitor(
            processManager,
            healthChecker,
            router,
            processManager
        );
    }

    public void run(
        int count,
        int startingPort,
        int coordinatorPort,
        int replicationFactor
    ) throws Exception {
        List<NodeInfo> allNodes = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            allNodes.add(new NodeInfo("node-" + i, startingPort + i - 1));
        }

        for (NodeInfo node : allNodes) {
            router.addNode(node.id());
        }

        for (NodeInfo self : allNodes) {
            List<String> replicaIds = replicasFor(self.id(), replicationFactor);
            String peerArg = replicaIds.stream()
                .map(id -> id + "=http://localhost:" + portFor(allNodes, id))
                .collect(Collectors.joining(","));
            processManager.spawnNode(self.id(), self.port(), peerArg);
        }

        healthMonitor.start(2, 5);
        startHttpServer(coordinatorPort, replicationFactor, allNodes);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            healthMonitor.stop();
            processManager.shutdownAll();
        }));

        logger.info(
            "Coordinator running with {} node(s), replication factor {}, on port {}",
            count, replicationFactor, coordinatorPort
        );
        shutdownLatch.await();
    }

    private void startHttpServer(
        int port,
        int replicationFactor,
        List<NodeInfo> nodes
    ) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/",
            new RouteRedirectHandler(router, healthChecker, processManager, replicationFactor)
        );
        server.createContext("/nodes",
            new ListNodesHandler(nodes, healthMonitor)
        );
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        logger.info("Coordinator HTTP server listening on port {}", port);
    }

    private List<String> replicasFor(String nodeId, int replicationFactor) {
        List<String> ring = router.routeFor(nodeId, replicationFactor + 1);
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

    @SuppressWarnings("unused")
    private void waitUntilHealthy() throws InterruptedException {
        Thread.sleep(2000);
        processManager.listNodes().forEach((id, handle) -> {
            boolean healthy = healthChecker.healthCheck(id);
            logger.info("{} on port {} -> healthy={}", id, handle.port(), healthy);
        });
    }
}
