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
import src.main.java.kvcluster.coordinator.domain.NodeLifecycle;
import src.main.java.kvcluster.coordinator.domain.NodeRegistry;
import src.main.java.kvcluster.coordinator.domain.NodeRouter;
import src.main.java.kvcluster.coordinator.domain.model.NodeInfo;
import src.main.java.kvcluster.coordinator.infra.ConsistentNodeHashService;
import src.main.java.kvcluster.coordinator.infra.NodeHealthProbe;
import src.main.java.kvcluster.coordinator.infra.NodeProcessManager;
import src.main.java.kvcluster.coordinator.transport.ListNodesHandler;
import src.main.java.kvcluster.coordinator.transport.RouteRedirectHandler;

/**
 * Composition root — the only class that instantiates infra concretions
 * and wires them to domain ports.
 * No other class in this module does 'new' on a dependency.
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
            processManager,   // NodeRegistry
            healthChecker,    // HealthChecker
            router,           // NodeRouter
            processManager    // NodeLifecycle
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
        List<NodeInfo> allNodes
    ) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RouteRedirectHandler(
            router,
            healthChecker,
            processManager,
            healthMonitor,
            replicationFactor
        ));
        server.createContext("/nodes", new ListNodesHandler(allNodes, healthMonitor));
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
}
