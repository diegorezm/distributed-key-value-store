package src.main.java.kvcluster.shared.models;

public record GetResponse(boolean found, String key, String value) {}
