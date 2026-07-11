package coordinator;

import coordinator.services.ConsistentNodeHashService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeHealthMonitor {

    private static final Logger logger = LoggerFactory.getLogger(
        NodeHealthMonitor.class
    );
    private static final int MAX_RESTART_ATTEMPTS = 1;

    private final NodeProcessManager processManager;
    private final ConsistentNodeHashService nodeHashService;
    private final Map<String, Boolean> healthState = new ConcurrentHashMap<>();
    private final Map<String, Integer> restartAttempts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().factory()
        );

    public NodeHealthMonitor(
        NodeProcessManager processManager,
        ConsistentNodeHashService nodeHashService
    ) {
        this.processManager = processManager;
        this.nodeHashService = nodeHashService;
    }

    /** Starts polling after {@code initialDelay} every {@code intervalSeconds}, running forever in the background. */
    public void start(int initialDelay, int intervalSeconds) {
        scheduler.scheduleAtFixedRate(
            this::checkAllNodes,
            initialDelay,
            intervalSeconds,
            TimeUnit.SECONDS
        );
        logger.info(
            "Health monitor started, checking every {}s",
            intervalSeconds
        );
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    public boolean isHealthy(String nodeId) {
        return healthState.getOrDefault(nodeId, false);
    }

    private void checkAllNodes() {
        processManager.listNodes().forEach((id, handle) -> {
            boolean healthyNow = processManager.healthCheck(id);
            Boolean healthyBefore = healthState.put(id, healthyNow);

            boolean isFirstCheck = healthyBefore == null;
            boolean stateChanged = !isFirstCheck && healthyBefore != healthyNow;

            if (healthyNow) {
                if (isFirstCheck || stateChanged) {
                    logger.info("{} on port {} is UP", id, handle.port());
                }
                if (stateChanged) {
                    nodeHashService.addNode(id);
                }
                restartAttempts.remove(id); // reset once healthy again
                return;
            }

            // node is unhealthy
            if (isFirstCheck || stateChanged) {
                logger.warn("{} on port {} is DOWN", id, handle.port());
                nodeHashService.removeNode(id);
            }

            int attempts = restartAttempts.getOrDefault(id, 0);
            if (attempts < MAX_RESTART_ATTEMPTS) {
                restartAttempts.put(id, attempts + 1);
                logger.warn(
                    "Attempting restart of node [{}] (attempt {}/{})",
                    id,
                    attempts + 1,
                    MAX_RESTART_ATTEMPTS
                );
                try {
                    processManager.restartNode(id);
                    healthState.put(id, false); // will be re-checked next cycle; assume still down until confirmed
                } catch (Exception e) {
                    logger.error(
                        "Failed to restart node [{}]: {}",
                        id,
                        e.getMessage()
                    );
                }
            } else {
                logger.error(
                    "Node [{}] exceeded restart attempts, evicting from pool permanently",
                    id
                );
                processManager.evict(id);
                healthState.remove(id);
                restartAttempts.remove(id);
                nodeHashService.removeNode(id); // already removed above, but harmless if called twice
            }
        });
    }
}
