package src.main.java.kvcluster.coordinator.domain;

import java.util.List;

/**
 * PORT — consistent-hash ring operations.
 * Implementations live in infra/.
 */
public interface NodeRouter {
    void addNode(String nodeId);
    void removeNode(String nodeId);
    List<String> routeFor(String key, int replicaCount);
    String routeFor(String key);
}
