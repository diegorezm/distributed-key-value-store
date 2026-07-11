package src.main.java.kvcluster.node.domain;

/**
 * PORT — defines the key-value store contract the domain exposes.
 * Implementations live in infra/.
 */
public interface KVService {
    void put(String key, String value);
    void del(String key);
    String get(String key);
}
