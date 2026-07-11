package src.main.java.kvcluster.coordinator.domain;

import java.util.Map;
import src.main.java.kvcluster.coordinator.domain.model.NodeHandle;

/**
 * PORT — read access to the live set of registered nodes.
 * Implementations live in infra/.
 */
public interface NodeRegistry {
    NodeHandle getNode(String id);
    Map<String, NodeHandle> listNodes();
    boolean isAlive(String id);
}
