package src.main.java.kvcluster.coordinator.infra;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import src.main.java.kvcluster.coordinator.domain.NodeLifecycle;
import src.main.java.kvcluster.coordinator.domain.NodeRegistry;
import src.main.java.kvcluster.coordinator.domain.model.NodeHandle;

/**
 * ADAPTER — manages OS-level node processes.
 * Implements NodeLifecycle (spawn/restart/evict/stop) and NodeRegistry (read access).
 * HTTP health-check logic lives in NodeHealthProbe, not here.
 */
public class NodeProcessManager implements NodeLifecycle, NodeRegistry {

    private static final Logger logger = LoggerFactory.getLogger(NodeProcessManager.class);

    private final Map<String, NodeHandle> nodes = new ConcurrentHashMap<>();
    private final Map<String, SpawnParams> spawnParams = new ConcurrentHashMap<>();
    private final Path nodeJarPath;

    private record SpawnParams(int port, String peerArg) {}

    public NodeProcessManager(Path nodeJarPath) {
        this.nodeJarPath = nodeJarPath;
    }

    @Override
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

    @Override
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

    @Override
    public void evict(String id) {
        stopNode(id);
        spawnParams.remove(id);
        logger.warn("Node [{}] permanently evicted from the pool", id);
    }

    @Override
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
    public void shutdownAll() {
        nodes.keySet().toArray(new String[0]);
        new java.util.ArrayList<>(nodes.keySet()).forEach(this::stopNode);
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
