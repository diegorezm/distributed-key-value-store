package src.main.java.kvcluster.coordinator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import src.main.java.kvcluster.coordinator.domain.HealthChecker;
import src.main.java.kvcluster.coordinator.domain.NodeLifecycle;
import src.main.java.kvcluster.coordinator.domain.NodeRegistry;
import src.main.java.kvcluster.coordinator.domain.NodeRouter;
import src.main.java.kvcluster.coordinator.domain.model.NodeHandle;

/**
 * Orchestrates cluster health over time.
 * Polls every node via HealthChecker, updates the ring via NodeRouter,
 * and triggers restarts or evictions via NodeLifecycle.
 *
 * Depends only on domain ports — no infra classes.
 * This makes it fully unit-testable without spawning processes or touching the network.
 */
public class NodeHealthMonitor {

    private static final Logger logger = LoggerFactory.getLogger(NodeHealthMonitor.class);
    private static final int MAX_RESTART_ATTEMPTS = 1;

    private final NodeRegistry registry;
    private final HealthChecker healthChecker;
    private final NodeRouter router;
    private final NodeLifecycle lifecycle;
    private final Map<String, Boolean> healthState = new ConcurrentHashMap<>();
    private final Map<String, Integer> restartAttempts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());

    public NodeHealthMonitor(
        NodeRegistry registry,
        HealthChecker healthChecker,
        NodeRouter router,
        NodeLifecycle lifecycle
    ) {
        this.registry = registry;
        this.healthChecker = healthChecker;
        this.router = router;
        this.lifecycle = lifecycle;
    }

    public void start(int initialDelay, int intervalSeconds) {
        scheduler.scheduleAtFixedRate(
            this::checkAllNodes,
            initialDelay,
            intervalSeconds,
            TimeUnit.SECONDS
        );
        logger.info("Health monitor started, checking every {}s", intervalSeconds);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    public boolean isHealthy(String nodeId) {
        return healthState.getOrDefault(nodeId, false);
    }

    private void checkAllNodes() {
        for (Map.Entry<String, NodeHandle> entry : registry.listNodes().entrySet()) {
            String id = entry.getKey();
            NodeHandle handle = entry.getValue();

            boolean healthyNow = healthChecker.healthCheck(id);
            Boolean healthyBefore = healthState.put(id, healthyNow);

            boolean isFirstCheck = healthyBefore == null;
            boolean stateChanged = !isFirstCheck && healthyBefore != healthyNow;

            if (healthyNow) {
                if (isFirstCheck || stateChanged) {
                    logger.info("{} on port {} is UP", id, handle.port());
                }
                if (stateChanged) {
                    router.addNode(id);
                }
                restartAttempts.remove(id);
                continue;
            }

            // node is unhealthy
            if (isFirstCheck || stateChanged) {
                logger.warn("{} on port {} is DOWN", id, handle.port());
                router.removeNode(id);
            }

            int attempts = restartAttempts.getOrDefault(id, 0);
            if (attempts < MAX_RESTART_ATTEMPTS) {
                restartAttempts.put(id, attempts + 1);
                logger.warn(
                    "Attempting restart of node [{}] (attempt {}/{})",
                    id, attempts + 1, MAX_RESTART_ATTEMPTS
                );
                try {
                    lifecycle.restartNode(id);
                    healthState.put(id, false);
                } catch (Exception e) {
                    logger.error("Failed to restart node [{}]: {}", id, e.getMessage());
                }
            } else {
                logger.error(
                    "Node [{}] exceeded restart attempts, evicting permanently", id
                );
                lifecycle.evict(id);
                healthState.remove(id);
                restartAttempts.remove(id);
            }
        }
    }
}
