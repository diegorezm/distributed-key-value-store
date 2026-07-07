package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import http.HttpResponseWriter;
import java.io.IOException;

public class HealthNodeServerHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpResponseWriter.send(exchange, 200, "OK");
    }
}
