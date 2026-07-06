package server;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StoreNodeServer {

    private static final Logger logger = LoggerFactory.getLogger(
        StoreNodeServer.class
    );

    private final int port;
    private final String id = "askdjasdl";

    public StoreNodeServer(int port) {
        this.port = port;
    }

    public void run() {
        try {
            HttpServer server = HttpServer.create(
                new InetSocketAddress(this.port),
                0
            );

            server.createContext(
                "/",
                new StoreNodeServerHandler(this.port, this.id)
            );
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
