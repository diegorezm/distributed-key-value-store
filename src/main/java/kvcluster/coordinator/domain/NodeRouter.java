package src.main.java.kvcluster.coordinator.domain;

import java.util.List;

/**
 * PORT — consistent-hash ring operations.
 * Implemented by ConsistentNodeHashService.
 */
public interface NodeRouter {
    void addNode(String nodeId);
    void removeNode(String nodeId);
    List<String> routeFor(String key, int count);
}
