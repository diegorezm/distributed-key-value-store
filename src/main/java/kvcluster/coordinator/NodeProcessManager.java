package src.main.java.kvcluster.coordinator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import src.main.java.kvcluster.coordinator.domain.NodeRegistry;

/**
 * Manages OS-level node processes.
 * Implements NodeRegistry so other components can query the live node set
 * without depending on this concrete class.
 */
public class NodeProcessManager implements NodeRegistry {

    private static final Logger logger = LoggerFactory.getLogger(NodeProcessManager.class);

    private final Map<String, NodeHandle> nodes = new ConcurrentHashMap<>();
    private final Map<String, SpawnParams> spawnParams = new ConcurrentHashMap<>();
    private final Path nodeJarPath;

    private record SpawnParams(int port, String peerArg) {}

    public NodeProcessManager(Path nodeJarPath) {
        this.nodeJarPath = nodeJarPath;
    }

    /**
     * Spawns a node process with the given id, port, and peer topology.
     * peerArg is expected to already be formatted as "node-2=http://localhost:4001,node-3=..."
     */
    public NodeHandle spawnNode(String id, int port, String peerArg) {
        if (nodes.containsKey(id)) {
            throw new IllegalStateException("Node " + id + " already exists");
        }

        spawnParams.put(id, new SpawnParams(port, peerArg));

        try {
            ProcessBuilder pb = new ProcessBuilder(
                "jbang",
                nodeJarPath.toString(),
                "--port=" + port,
                "--id=" + id,
                "--peers=" + peerArg
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            NodeHandle handle = new NodeHandle(id, port, process);
            nodes.put(id, handle);

            pipeOutput(id, process);

            logger.info("Spawned node [{}] on port {} (pid={})", id, port, process.pid());
            return handle;
        } catch (IOException e) {
            throw new RuntimeException("Failed to spawn node " + id, e);
        }
    }

    /** Kills the existing process (if any) and spawns a fresh one with the same original params. */
    public NodeHandle restartNode(String id) {
        SpawnParams params = spawnParams.get(id);
        if (params == null) {
            throw new IllegalStateException("No spawn params recorded for node " + id);
        }
        if (nodes.containsKey(id)) {
            stopNode(id);
        }
        logger.info("Restarting node [{}] on port {}", id, params.port());
        return spawnNode(id, params.port(), params.peerArg());
    }

    /** Permanently removes a node — no restart, no further health checks. */
    public void evict(String id) {
        stopNode(id);
        spawnParams.remove(id);
        logger.warn("Node [{}] permanently evicted from the pool", id);
    }

    public void stopNode(String id) {
        NodeHandle handle = nodes.remove(id);
        if (handle == null) {
            logger.warn("Tried to stop unknown node [{}]", id);
            return;
        }
        ProcessHandle ph = handle.process().toHandle();
        ph.descendants().forEach(ProcessHandle::destroy);
        handle.process().destroy();
        try {
            boolean exited = handle.process().waitFor(3, TimeUnit.SECONDS);
            if (!exited) {
                logger.warn("Node [{}] didn't exit gracefully, forcing", id);
                ph.descendants().forEach(ProcessHandle::destroyForcibly);
                handle.process().destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        logger.info("Stopped node [{}]", id);
    }

    @Override
    public boolean isAlive(String id) {
        NodeHandle handle = nodes.get(id);
        return handle != null && handle.process().isAlive();
    }

    @Override
    public NodeHandle getNode(String id) {
        return nodes.get(id);
    }

    @Override
    public Map<String, NodeHandle> listNodes() {
        return Map.copyOf(nodes);
    }

    public void shutdownAll() {
        nodes.keySet().forEach(this::stopNode);
    }

    private void pipeOutput(String id, Process process) {
        Executors.newVirtualThreadPerTaskExecutor().submit(() -> {
            try (var reader = process.inputReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("[{}] {}", id, line);
                }
            } catch (IOException e) {
                logger.warn("[{}] Output stream closed: {}", id, e.getMessage());
            }
        });
    }
}
