package src.main.java.kvcluster.node.domain.model;

public record WalEntry(String op, String key, String value) {
    public static WalEntry put(String key, String value) {
        return new WalEntry("PUT", key, value);
    }
    public static WalEntry delete(String key) {
        return new WalEntry("DELETE", key, null);
    }
}
