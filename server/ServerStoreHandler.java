package server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dto.DelRequestDTO;
import dto.GetRequestDTO;
import dto.PutRequestDTO;
import http.HttpResponseWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ServerStoreHandler implements HttpHandler {

    private static final Gson GSON = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String body;

        try (InputStream is = exchange.getRequestBody()) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        switch (exchange.getRequestMethod()) {
            case "POST" -> {
                PutRequestDTO putReq = GSON.fromJson(body, PutRequestDTO.class);

                IO.println(
                    "key: " + putReq.key() + " value: " + putReq.value()
                );

                HttpResponseWriter.send(exchange, 201, "Object created!!");
            }
            case "GET" -> {
                GetRequestDTO getReq = GSON.fromJson(body, GetRequestDTO.class);

                HttpResponseWriter.send(exchange, 200, "This is your object");
            }
            case "DELETE" -> {
                DelRequestDTO delReq = GSON.fromJson(body, DelRequestDTO.class);

                HttpResponseWriter.send(exchange, 200, "Object deleted!!");
            }
            default -> {
                HttpResponseWriter.send(
                    exchange,
                    405,
                    "Operation not supported!!"
                );
            }
        }
    }
}
