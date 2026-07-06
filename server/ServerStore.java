package server;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class ServerStore {

    private final int port;

    public ServerStore(int port) {
        this.port = port;
    }

    public void run() {
        try {
            HttpServer server = HttpServer.create(
                new InetSocketAddress(this.port),
                0
            );

            server.createContext("/", new ServerStoreHandler());
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            server.start();

            IO.println(
                "SERVER RUNNING ON: " +
                    server.getAddress().getHostString() +
                    ":" +
                    this.port
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
