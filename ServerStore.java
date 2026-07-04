import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * ServerStore
 */
public class ServerStore {

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

    static class ServerStoreHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body;
            try (InputStream is = exchange.getRequestBody()) {
                body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            Gson gson = new Gson();

            switch (exchange.getRequestMethod()) {
                case "POST":
                    var putReq = gson.fromJson(body, PutRequest.class);
                    IO.println(
                        "key: " +
                            putReq.key() +
                            " " +
                            "value: " +
                            putReq.value()
                    );
                    HttpResponse.send(exchange, "Object created!!");
                    break;
                case "GET":
                    var getReq = gson.fromJson(body, GetRequest.class);
                    HttpResponse.send(exchange, "This is your object");
                    break;
                case "DELETE":
                    var delReq = gson.fromJson(body, DelRequest.class);
                    HttpResponse.send(exchange, "Object deleeted!!!");
                    break;
                default:
                    HttpResponse.send(exchange, "Operation not supported!!");
                    break;
            }
        }
    }
}
