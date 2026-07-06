package server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dto.DelRequestDTO;
import dto.GetRequestDTO;
import dto.PutRequestDTO;
import http.HttpResponseWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StoreNodeServerHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(
        StoreNodeServerHandler.class
    );
    private static final Gson GSON = new Gson();

    private final int port;
    private final String serverId;

    public StoreNodeServerHandler(int port, String serverId) {
        this.port = port;
        this.serverId = serverId;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String body;
            try (InputStream is = exchange.getRequestBody()) {
                body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            switch (exchange.getRequestMethod()) {
                case "POST" -> handlePut(exchange, body);
                case "GET" -> handleGet(exchange, body);
                case "DELETE" -> handleDelete(exchange, body);
                default -> {
                    logger.warn(
                        "[{}:{}] Unsupported method: {}",
                        serverId,
                        port,
                        exchange.getRequestMethod()
                    );
                    HttpResponseWriter.send(
                        exchange,
                        405,
                        "Operation not supported!!"
                    );
                }
            }
        } catch (JsonSyntaxException e) {
            logger.warn(
                "[{}:{}] Malformed JSON: {}",
                serverId,
                port,
                e.getMessage()
            );
            HttpResponseWriter.send(exchange, 400, "Malformed JSON body");
        } catch (Exception e) {
            logger.error(
                "[{}:{}] Unhandled error processing request",
                serverId,
                port,
                e
            );
            HttpResponseWriter.send(exchange, 500, "Internal server error");
        }
    }

    private void handlePut(HttpExchange exchange, String body)
        throws IOException {
        PutRequestDTO putReq = GSON.fromJson(body, PutRequestDTO.class);
        if (putReq == null || putReq.key() == null || putReq.value() == null) {
            logger.warn("[{}:{}] PUT missing key or value", serverId, port);
            HttpResponseWriter.send(exchange, 400, "Missing key or value");
            return;
        }
        logger.info(
            "[{}:{}] PUT key={} value={}",
            serverId,
            port,
            putReq.key(),
            putReq.value()
        );
        HttpResponseWriter.send(exchange, 201, "Object created!!");
    }

    private void handleGet(HttpExchange exchange, String body)
        throws IOException {
        GetRequestDTO getReq = GSON.fromJson(body, GetRequestDTO.class);
        if (getReq == null || getReq.key() == null) {
            logger.warn("[{}:{}] GET missing key", serverId, port);
            HttpResponseWriter.send(exchange, 400, "Missing key");
            return;
        }
        logger.info("[{}:{}] GET key={}", serverId, port, getReq.key());
        HttpResponseWriter.send(exchange, 200, "This is your object");
    }

    private void handleDelete(HttpExchange exchange, String body)
        throws IOException {
        DelRequestDTO delReq = GSON.fromJson(body, DelRequestDTO.class);
        if (delReq == null || delReq.key() == null) {
            logger.warn("[{}:{}] DELETE missing key", serverId, port);
            HttpResponseWriter.send(exchange, 400, "Missing key");
            return;
        }
        logger.info("[{}:{}] DELETE key={}", serverId, port, delReq.key());
        HttpResponseWriter.send(exchange, 200, "Object deleted!!");
    }
}
