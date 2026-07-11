package src.main.java.kvcluster.coordinator.domain.model;

/**
 * Value type representing a live node: its logical id, TCP port, and OS process.
 */
public record NodeHandle(String id, int port, Process process) {}
