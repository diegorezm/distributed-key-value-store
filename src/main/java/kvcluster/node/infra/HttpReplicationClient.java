package src.main.java.kvcluster.node.infra;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import src.main.java.kvcluster.node.domain.ReplicationClient;

/**
 * ADAPTER — implements ReplicationClient by firing async HTTP requests to peer nodes.
 */
public class HttpReplicationClient implements ReplicationClient {

    private static final Logger logger = LoggerFactory.getLogger(HttpReplicationClient.class);
    private static final String REPLICATION_HEADER = "X-Replication-Write";

    private final String nodeId;
    private final int port;
    private final Map<String, String> peers;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public HttpReplicationClient(String nodeId, int port, Map<String, String> peers) {
        this.nodeId = nodeId;
        this.port = port;
        this.peers = peers;
    }

    @Override
    public void replicate(String method, String body) {
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
                            nodeId, port, method, peerId, peerAddress, e.getMessage()
                        );
                        return null;
                    });

                logger.debug("[{}:{}] Replicating {} to {} ({})", nodeId, port, method, peerId, peerAddress);
            } catch (Exception e) {
                logger.warn(
                    "[{}:{}] Failed to build replication request for {} ({}): {}",
                    nodeId, port, peerId, peerAddress, e.getMessage()
                );
            }
        }
    }
}
