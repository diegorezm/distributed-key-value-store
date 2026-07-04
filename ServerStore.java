import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * ServerStore
 */
public class ServerStore {

    static class ServerStoreHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Hello, this is a simple HTTP server response!";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    private int port;

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

            server.setExecutor(null);
            server.start();

            System.out.println(
                "SERVER RUNNING ON: " +
                    server.getAddress().getHostString() +
                    ":" +
                    this.port
            );
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
