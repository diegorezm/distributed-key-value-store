package src.main.java.kvcluster.coordinator.domain;

import java.util.Map;
import src.main.java.kvcluster.coordinator.NodeHandle;

/**
 * PORT — read access to the live set of nodes.
 * Implemented by NodeProcessManager in production.
 */
public interface NodeRegistry {
    NodeHandle getNode(String id);
    Map<String, NodeHandle> listNodes();
    boolean isAlive(String id);
}
