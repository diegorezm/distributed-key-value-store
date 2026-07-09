package coordinator.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentNodeHashService {

    private final SortedMap<Long, String> ring = new TreeMap<>();
    private static final int VIRTUAL_NODES = 100;

    public void addNode(String nodeId) {
        for (int i = 0; i < VIRTUAL_NODES; i++) {
            long position = hash(nodeId + "#" + i);
            ring.put(position, nodeId);
        }
    }

    public void removeNode(String nodeId) {
        for (int i = 0; i < VIRTUAL_NODES; i++) {
            long position = hash(nodeId + "#" + i);
            ring.remove(position, nodeId);
        }
    }

    /**
     * Returns up to {@code replicaCount} distinct physical nodes for a key,
     * walking clockwise starting from the key's position on the ring.
     * The first entry is the primary owner; the rest are replicas.
     */
    public List<String> routeFor(String key, int replicaCount) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("No nodes registered");
        }
        long keyPosition = hash(key);
        Set<String> distinctNodes = new LinkedHashSet<>(); // preserves order, dedupes
        // Start from the key's position, walk clockwise, wrapping around once if needed.
        SortedMap<Long, String> clockwise = ring.tailMap(keyPosition);
        collect(clockwise.values(), distinctNodes, replicaCount);

        if (distinctNodes.size() < replicaCount) {
            // wrapped past the end of the ring — continue from the beginning
            collect(
                ring.headMap(keyPosition).values(),
                distinctNodes,
                replicaCount
            );
        }

        return new ArrayList<>(distinctNodes);
    }

    public String routeFor(String key) {
        return routeFor(key, 1).get(0);
    }

    private void collect(
        Iterable<String> positions,
        Set<String> acc,
        int limit
    ) {
        for (String nodeId : positions) {
            if (acc.size() >= limit) return;
            acc.add(nodeId); // LinkedHashSet: no-op if already present, keeps first-seen order
        }
    }

    private long hash(String input) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(input.getBytes(StandardCharsets.UTF_8));
            long h = 0;
            for (int i = 0; i < 8; i++) {
                h = (h << 8) | (digest[i] & 0xff);
            }
            return h;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
