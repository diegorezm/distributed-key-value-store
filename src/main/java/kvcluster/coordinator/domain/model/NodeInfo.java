package src.main.java.kvcluster.coordinator.domain.model;

/**
 * Value type representing a node's logical identity and address.
 * Used for initial topology setup and HTTP /nodes responses.
 */
public record NodeInfo(String id, int port) {}
