package node.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import node.services.KVStoreService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import shared.dto.DelRequestDTO;
import shared.dto.DeleteResponseDTO;
import shared.dto.GetRequestDTO;
import shared.dto.GetResponseDTO;
import shared.dto.PutRequestDTO;
import shared.dto.PutResponseDTO;
import shared.http.HttpResponseWriter;

public class NodeRequestHandler implements HttpHandler {

    private final KVStoreService kv = new KVStoreService();
    private static final Logger logger = LoggerFactory.getLogger(
        NodeRequestHandler.class
    );
    private static final Gson GSON = new Gson();

    private final int port;
    private final String serverId;

    public NodeRequestHandler(int port, String serverId) {
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
            exchange.getResponseHeaders().set("X-Node-Id", serverId);

            switch (exchange.getRequestMethod()) {
                case "POST" -> handlePut(exchange, body);
                case "PUT" -> handlePut(exchange, body);
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

        kv.put(putReq.key(), putReq.value());
        logger.info(
            "[{}:{}] PUT key={} value={}",
            serverId,
            port,
            putReq.key(),
            putReq.value()
        );
        HttpResponseWriter.send(
            exchange,
            201,
            new PutResponseDTO(true, putReq.key())
        );
    }

    private void handleGet(HttpExchange exchange, String body)
        throws IOException {
        GetRequestDTO getReq = GSON.fromJson(body, GetRequestDTO.class);
        if (getReq == null || getReq.key() == null) {
            logger.warn("[{}:{}] GET missing key", serverId, port);
            HttpResponseWriter.send(exchange, 400, "Missing key");
            return;
        }

        String value = kv.get(getReq.key());
        boolean found = value != null;
        logger.info(
            "[{}:{}] GET key={} found={}",
            serverId,
            port,
            getReq.key(),
            found
        );
        HttpResponseWriter.send(
            exchange,
            200,
            new GetResponseDTO(found, getReq.key(), value)
        );
    }

    private void handleDelete(HttpExchange exchange, String body)
        throws IOException {
        DelRequestDTO delReq = GSON.fromJson(body, DelRequestDTO.class);
        if (delReq == null || delReq.key() == null) {
            logger.warn("[{}:{}] DELETE missing key", serverId, port);
            HttpResponseWriter.send(exchange, 400, "Missing key");
            return;
        }

        kv.del(delReq.key());
        logger.info("[{}:{}] DELETE key={}", serverId, port, delReq.key());
        HttpResponseWriter.send(
            exchange,
            200,
            new DeleteResponseDTO(true, delReq.key())
        );
    }
}
