package node;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Executors;
import node.handlers.HealthNodeServerHandler;
import node.handlers.NodeRequestHandler;
import node.services.KVStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeServer {

    private static final Logger logger = LoggerFactory.getLogger(
        NodeServer.class
    );

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
            HttpServer server = HttpServer.create(
                new InetSocketAddress(this.port),
                0
            );

            logger.info("[{}:{}] Starting with peers: {}", id, port, peers);

            KVStoreService kv = new KVStoreService(id);

            server.createContext(
                "/",
                new NodeRequestHandler(this.port, this.id, this.peers, kv)
            );
            server.createContext("/health", new HealthNodeServerHandler());
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            server.start();

            logger.info(
                "Server running on {}:{}",
                server.getAddress().getHostString(),
                this.port
            );
        } catch (Exception e) {
            logger.error("Failed to start node [{}] on port {}", id, port, e);
        }
    }
}
