package services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    public String routeFor(String key) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("No nodes registered");
        }
        long keyPosition = hash(key);
        SortedMap<Long, String> clockwise = ring.tailMap(keyPosition);
        // if you find nothing, go back to the very first position.
        long targetPosition = clockwise.isEmpty()
            ? ring.firstKey()
            : clockwise.firstKey();
        return ring.get(targetPosition);
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
