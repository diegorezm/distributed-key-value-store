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

    public void run(int count, int startingPort, int coordinatorPort) throws Exception {
        List<NodeInfo> allNodes = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            allNodes.add(new NodeInfo("node-" + i, startingPort + i - 1));
        }

        for (NodeInfo self : allNodes) {
            String peerArg = allNodes.stream()
                .filter(n -> !n.id().equals(self.id()))
                .map(n -> n.id() + "=http://localhost:" + n.port())
                .collect(Collectors.joining(","));

            processManager.spawnNode(self.id(), self.port(), peerArg);
            nodeHashService.addNode(self.id());
        }

        waitUntilHealthy();
        startHttpServer(coordinatorPort);

        Runtime.getRuntime().addShutdownHook(new Thread(processManager::shutdownAll));

        logger.info("Coordinator running with {} node(s) on port {}", count, coordinatorPort);
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
}
