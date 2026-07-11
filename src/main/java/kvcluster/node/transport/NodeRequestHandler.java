package src.main.java.kvcluster.node.transport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import src.main.java.kvcluster.node.domain.KVService;
import src.main.java.kvcluster.node.domain.ReplicationClient;
import src.main.java.kvcluster.shared.models.DelRequest;
import src.main.java.kvcluster.shared.models.DeleteResponse;
import src.main.java.kvcluster.shared.models.GetRequest;
import src.main.java.kvcluster.shared.models.GetResponse;
import src.main.java.kvcluster.shared.models.PutRequest;
import src.main.java.kvcluster.shared.models.PutResponse;
import src.main.java.kvcluster.shared.http.HttpResponseWriter;

/**
 * TRANSPORT — parses incoming HTTP requests and delegates to KVService.
 * Contains no business logic.
 */
public class NodeRequestHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(NodeRequestHandler.class);
    private static final Gson GSON = new Gson();
    private static final String REPLICATION_HEADER = "X-Replication-Write";

    private final int port;
    private final String serverId;
    private final KVService kv;
    private final ReplicationClient replicationClient;

    public NodeRequestHandler(int port, String serverId, KVService kv, ReplicationClient replicationClient) {
        this.port = port;
        this.serverId = serverId;
        this.kv = kv;
        this.replicationClient = replicationClient;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String body;
            try (InputStream is = exchange.getRequestBody()) {
                body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            exchange.getResponseHeaders().set("X-Node-Id", serverId);

            boolean isReplicationWrite = "true".equals(
                exchange.getRequestHeaders().getFirst(REPLICATION_HEADER)
            );

            switch (exchange.getRequestMethod()) {
                case "POST", "PUT" -> handlePut(exchange, body, isReplicationWrite);
                case "GET"         -> handleGet(exchange, body);
                case "DELETE"      -> handleDelete(exchange, body, isReplicationWrite);
                default -> {
                    logger.warn("[{}:{}] Unsupported method: {}", serverId, port, exchange.getRequestMethod());
                    HttpResponseWriter.send(exchange, 405, "Operation not supported!!");
                }
            }
        } catch (JsonSyntaxException e) {
            logger.warn("[{}:{}] Malformed JSON: {}", serverId, port, e.getMessage());
            HttpResponseWriter.send(exchange, 400, "Malformed JSON body");
        } catch (Exception e) {
            logger.error("[{}:{}] Unhandled error processing request", serverId, port, e);
            HttpResponseWriter.send(exchange, 500, "Internal server error");
        }
    }

    private void handlePut(HttpExchange exchange, String body, boolean isReplicationWrite) throws IOException {
        PutRequest req = GSON.fromJson(body, PutRequest.class);
        if (req == null || req.key() == null || req.value() == null) {
            logger.warn("[{}:{}] PUT missing key or value", serverId, port);
            HttpResponseWriter.send(exchange, 400, "Missing key or value");
            return;
        }
        kv.put(req.key(), req.value());
        logger.info("[{}:{}] PUT key={} value={}", serverId, port, req.key(), req.value());
        if (!isReplicationWrite) {
            replicationClient.replicate("PUT", body);
        }
        HttpResponseWriter.send(exchange, 201, new PutResponse(true, req.key()));
    }

    private void handleGet(HttpExchange exchange, String body) throws IOException {
        GetRequest req = GSON.fromJson(body, GetRequest.class);
        if (req == null || req.key() == null) {
            logger.warn("[{}:{}] GET missing key", serverId, port);
            HttpResponseWriter.send(exchange, 400, "Missing key");
            return;
        }
        String value = kv.get(req.key());
        boolean found = value != null;
        logger.info("[{}:{}] GET key={} found={}", serverId, port, req.key(), found);
        HttpResponseWriter.send(exchange, 200, new GetResponse(found, req.key(), value));
    }

    private void handleDelete(HttpExchange exchange, String body, boolean isReplicationWrite) throws IOException {
        DelRequest req = GSON.fromJson(body, DelRequest.class);
        if (req == null || req.key() == null) {
            logger.warn("[{}:{}] DELETE missing key", serverId, port);
            HttpResponseWriter.send(exchange, 400, "Missing key");
            return;
        }
        kv.del(req.key());
        logger.info("[{}:{}] DELETE key={}", serverId, port, req.key());
        if (!isReplicationWrite) {
            replicationClient.replicate("DELETE", body);
        }
        HttpResponseWriter.send(exchange, 200, new DeleteResponse(true, req.key()));
    }
}
