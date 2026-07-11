package src.main.java.kvcluster.coordinator.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import src.main.java.kvcluster.coordinator.domain.NodeRouter;

/**
 * Consistent-hash ring implementation.
 * Now implements NodeRouter so dependents program to the interface.
 */
public class ConsistentNodeHashService implements NodeRouter {

    private static final int VIRTUAL_NODES = 100;
    private final TreeMap<Integer, String> ring = new TreeMap<>();

    @Override
    public void addNode(String nodeId) {
        for (int i = 0; i < VIRTUAL_NODES; i++) {
            ring.put(hash(nodeId + "-" + i), nodeId);
        }
    }

    @Override
    public void removeNode(String nodeId) {
        for (int i = 0; i < VIRTUAL_NODES; i++) {
            ring.remove(hash(nodeId + "-" + i));
        }
    }

    /**
     * Returns up to {@code count} distinct node IDs clockwise from the key's hash position.
     */
    @Override
    public List<String> routeFor(String key, int count) {
        if (ring.isEmpty()) return List.of();

        List<String> result = new ArrayList<>();
        int keyHash = hash(key);

        // Start at the first entry >= keyHash, wrap around if needed
        Map.Entry<Integer, String> entry = ring.ceilingEntry(keyHash);
        if (entry == null) entry = ring.firstEntry();

        // Walk the ring clockwise, collecting distinct node IDs
        Integer startKey = entry.getKey();
        do {
            String nodeId = entry.getValue();
            if (!result.contains(nodeId)) {
                result.add(nodeId);
            }
            if (result.size() == count) break;

            Map.Entry<Integer, String> next = ring.higherEntry(entry.getKey());
            entry = (next != null) ? next : ring.firstEntry();
        } while (!entry.getKey().equals(startKey));

        return result;
    }

    private int hash(String key) {
        // FNV-1a 32-bit — fast, low collision, no external deps
        int h = 0x811c9dc5;
        for (byte b : key.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            h ^= (b & 0xff);
            h *= 0x01000193;
        }
        return h;
    }
}
