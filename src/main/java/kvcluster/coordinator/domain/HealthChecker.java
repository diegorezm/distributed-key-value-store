package src.main.java.kvcluster.coordinator.domain;

/**
 * PORT — asks a single node whether it is ready to serve traffic.
 * Implementations live in infra/.
 */
public interface HealthChecker {
    boolean healthCheck(String nodeId);
}
