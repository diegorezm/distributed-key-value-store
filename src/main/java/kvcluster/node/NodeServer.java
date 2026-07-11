package src.main.java.kvcluster.node;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;

import src.main.java.kvcluster.node.domain.KVService;
import src.main.java.kvcluster.node.domain.ReplicationClient;
import src.main.java.kvcluster.node.infra.DefaultKVService;
import src.main.java.kvcluster.node.infra.FileWalWriter;
import src.main.java.kvcluster.node.infra.HttpReplicationClient;
import src.main.java.kvcluster.node.transport.HealthHandler;
import src.main.java.kvcluster.node.transport.NodeRequestHandler;

/**
 * Composition root for the node — wires domain ports to infra adapters and starts the HTTP server.
 */
public class NodeServer {

    private static final Logger logger = LoggerFactory.getLogger(NodeServer.class);

    private final int port;
    private final String id;
    private final Map<String, String> peers;

    public NodeServer(int port, String id, Map<String, String> peers) {
        this.port = port;
        this.id = id;
        this.peers = peers;
    }

    public void run() {
        try {
            // --- Infra adapters ---
            FileWalWriter wal = new FileWalWriter(id);
            KVService kv = new DefaultKVService(wal);
            ReplicationClient replication = new HttpReplicationClient(id, port, peers);

            // --- HTTP server ---
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            logger.info("[{}:{}] Starting with peers: {}", id, port, peers);

            server.createContext("/", new NodeRequestHandler(port, id, kv, replication));
            server.createContext("/health", new HealthHandler());
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            server.start();

            logger.info("Server running on {}:{}", server.getAddress().getHostString(), port);
            Thread.currentThread().join();
        } catch (Exception e) {
            logger.error("Failed to start node [{}] on port {}", id, port, e);
        }
    }
}
