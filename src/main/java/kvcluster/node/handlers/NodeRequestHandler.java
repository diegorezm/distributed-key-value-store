package src.main.java.kvcluster.node.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import src.main.java.kvcluster.node.services.KVStoreService;
import src.main.java.kvcluster.shared.models.DelRequest;
import src.main.java.kvcluster.shared.models.DeleteResponse;
import src.main.java.kvcluster.shared.models.GetRequest;
import src.main.java.kvcluster.shared.models.GetResponse;
import src.main.java.kvcluster.shared.models.PutRequest;
import src.main.java.kvcluster.shared.models.PutResponse;
import src.main.java.kvcluster.shared.http.HttpResponseWriter;

public class NodeRequestHandler implements HttpHandler {

    private final KVStoreService kv;
    private static final Logger logger = LoggerFactory.getLogger(
        NodeRequestHandler.class
    );
    private static final Gson GSON = new Gson();
    private static final String REPLICATION_HEADER = "X-Replication-Write";

    private final int port;
    private final String serverId;
    private final Map<String, String> peers;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public NodeRequestHandler(int port, String serverId, Map<String, String> peers, KVStoreService kv) {
        this.port = port;
        this.serverId = serverId;
        this.peers = peers;
        this.kv = kv;
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
                case "GET" -> handleGet(exchange, body);
                case "DELETE" -> handleDelete(exchange, body, isReplicationWrite);
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

    private void handlePut(HttpExchange exchange, String body, boolean isReplicationWrite)
        throws IOException {
        PutRequest putReq = GSON.fromJson(body, PutRequest.class);
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

        if (!isReplicationWrite) {
            replicateToPeers("PUT", body);
        }

        HttpResponseWriter.send(
            exchange,
            201,
            new PutResponse(true, putReq.key())
        );
    }

    private void handleGet(HttpExchange exchange, String body)
        throws IOException {
        GetRequest getReq = GSON.fromJson(body, GetRequest.class);
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
            new GetResponse(found, getReq.key(), value)
        );
    }

    private void handleDelete(HttpExchange exchange, String body, boolean isReplicationWrite)
        throws IOException {
        DelRequest delReq = GSON.fromJson(body, DelRequest.class);
        if (delReq == null || delReq.key() == null) {
            logger.warn("[{}:{}] DELETE missing key", serverId, port);
            HttpResponseWriter.send(exchange, 400, "Missing key");
            return;
        }

        kv.del(delReq.key());
        logger.info("[{}:{}] DELETE key={}", serverId, port, delReq.key());

        if (!isReplicationWrite) {
            replicateToPeers("DELETE", body);
        }

        HttpResponseWriter.send(
            exchange,
            200,
            new DeleteResponse(true, delReq.key())
        );
    }

    private void replicateToPeers(String method, String body) {
        for (Map.Entry<String, String> peer : peers.entrySet()) {
            String peerId = peer.getKey();
            String peerAddress = peer.getValue();
            try {
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(peerAddress + "/"))
                    .header(REPLICATION_HEADER, "true")
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body))
                    .build();

                httpClient.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(e -> {
                        logger.warn(
                            "[{}:{}] Failed to replicate {} to {} ({}): {}",
                            serverId, port, method, peerId, peerAddress, e.getMessage()
                        );
                        return null;
                    });

                logger.debug("[{}:{}] Replicating {} to {} ({})", serverId, port, method, peerId, peerAddress);
            } catch (Exception e) {
                logger.warn(
                    "[{}:{}] Failed to build replication request for {} ({}): {}",
                    serverId, port, peerId, peerAddress, e.getMessage()
                );
            }
        }
    }
}
