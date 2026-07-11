package src.main.java.kvcluster.node.transport;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import src.main.java.kvcluster.shared.http.HttpResponseWriter;

/**
 * TRANSPORT — responds 200 OK to health checks from the coordinator.
 */
public class HealthHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpResponseWriter.send(exchange, 200, "OK");
    }
}
