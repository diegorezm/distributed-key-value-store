package src.main.java.kvcluster.coordinator.infra;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import src.main.java.kvcluster.coordinator.domain.NodeRouter;

/**
 * ADAPTER — consistent-hash ring implementation of NodeRouter.
 * Moved from services/ to infra/ to match the node module's layering convention.
 */
public class ConsistentNodeHashService implements NodeRouter {

    private final SortedMap<Long, String> ring = new TreeMap<>();
    private static final int VIRTUAL_NODES = 100;

    @Override
    public void addNode(String nodeId) {
        for (int i = 0; i < VIRTUAL_NODES; i++) {
            ring.put(hash(nodeId + "#" + i), nodeId);
        }
    }

    @Override
    public void removeNode(String nodeId) {
        for (int i = 0; i < VIRTUAL_NODES; i++) {
            ring.remove(hash(nodeId + "#" + i), nodeId);
        }
    }

    @Override
    public List<String> routeFor(String key, int replicaCount) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("No nodes registered");
        }
        long keyPosition = hash(key);
        Set<String> distinctNodes = new LinkedHashSet<>();
        SortedMap<Long, String> clockwise = ring.tailMap(keyPosition);
        collect(clockwise.values(), distinctNodes, replicaCount);
        if (distinctNodes.size() < replicaCount) {
            collect(ring.headMap(keyPosition).values(), distinctNodes, replicaCount);
        }
        return new ArrayList<>(distinctNodes);
    }

    @Override
    public String routeFor(String key) {
        return routeFor(key, 1).get(0);
    }

    private void collect(Iterable<String> positions, Set<String> acc, int limit) {
        for (String nodeId : positions) {
            if (acc.size() >= limit) return;
            acc.add(nodeId);
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
