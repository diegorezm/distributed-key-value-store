package src.main.java.kvcluster.coordinator;

public record NodeHandle(String id, int port, Process process) {}
