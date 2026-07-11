package src.main.java.kvcluster.coordinator.domain;

/**
 * PORT — asks a node whether it is ready to serve traffic.
 * Implemented by NodeHealthProbe in production (HTTP GET /health).
 * Can be replaced with a stub in tests.
 */
public interface HealthChecker {
    boolean healthCheck(String nodeId);
}
