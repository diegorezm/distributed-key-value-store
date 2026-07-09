package coordinator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import coordinator.services.ConsistentNodeHashService;

public class NodeHealthMonitor {
    private static final Logger logger = LoggerFactory.getLogger(NodeHealthMonitor.class);

    private final NodeProcessManager processManager;
    private final ConsistentNodeHashService nodeHashService;
    private final Map<String, Boolean> healthState = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());

    public NodeHealthMonitor(NodeProcessManager processManager, ConsistentNodeHashService router) {
        this.processManager = processManager;
        this.nodeHashService = router;
    }

    /** Starts polling every {@code intervalSeconds}, running forever in the background. */
     public void start(int intervalSeconds) {
         scheduler.scheduleAtFixedRate(
             this::checkAllNodes,
             0,
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
         processManager.listNodes().forEach((id, handle) -> {
             boolean healthyNow = processManager.healthCheck(id);
             Boolean healthyBefore = healthState.put(id, healthyNow);

             boolean isFirstCheck = healthyBefore == null;
             boolean stateChanged = !isFirstCheck && healthyBefore != healthyNow;

             if (isFirstCheck || stateChanged) {
                 if (healthyNow) {
                     logger.info("{} on port {} is UP", id, handle.port());
                     if (stateChanged) {
                         nodeHashService.addNode(id); // rejoin the ring if it had been removed
                     }
                 } else {
                     logger.warn("{} on port {} is DOWN", id, handle.port());
                     nodeHashService.removeNode(id); // pull out of routing immediately
                 }
             }
         });
     }

}
