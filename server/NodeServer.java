package server;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeServer {

    private static final Logger logger = LoggerFactory.getLogger(
        NodeServer.class
    );

    private final int port;
    private final String id;

    public NodeServer(int port, String id) {
        this.port = port;
        this.id = id;
    }

    public void run() {
        try {
            HttpServer server = HttpServer.create(
                new InetSocketAddress(this.port),
                0
            );

            server.createContext(
                "/",
                new NodeServerHandler(this.port, this.id)
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
            e.printStackTrace();
        }
    }
}
