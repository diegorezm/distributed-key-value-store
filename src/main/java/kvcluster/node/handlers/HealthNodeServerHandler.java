package src.main.java.kvcluster.node.handlers;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import src.main.java.kvcluster.shared.http.HttpResponseWriter;

public class HealthNodeServerHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpResponseWriter.send(exchange, 200, "OK");
    }
}
