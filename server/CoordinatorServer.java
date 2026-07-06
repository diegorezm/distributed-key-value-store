package server;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class CoordinatorServer {

    public static final int PORT = 8080;

    public void run() {
        try {
            HttpServer server = HttpServer.create(
                new InetSocketAddress(PORT),
                0
            );
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            server.start();

            IO.println(
                "COORDINATOR RUNNING ON: " +
                    server.getAddress().getHostString() +
                    ":" +
                    PORT
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
