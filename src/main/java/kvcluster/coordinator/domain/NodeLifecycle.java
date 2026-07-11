package src.main.java.kvcluster.coordinator.domain;

import src.main.java.kvcluster.coordinator.domain.model.NodeHandle;

/**
 * PORT — controls the lifecycle of a node (spawn, restart, evict, stop).
 * Implementations live in infra/.
 */
public interface NodeLifecycle {
    NodeHandle spawnNode(String id, int port, String peerArg);
    NodeHandle restartNode(String id);
    void evict(String id);
    void stopNode(String id);
    void shutdownAll();
}
