package src.test.java.kvcluster.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import src.main.java.kvcluster.shared.models.GetRequest;
import src.main.java.kvcluster.shared.models.GetResponse;
import src.main.java.kvcluster.shared.models.PutRequest;

@Tag("integration")
class ReplicationIntegrationTest {

    static TestCluster cluster;
    static final Gson GSON = new Gson();
    static final int NODE_START_PORT = 4500;
    static final int COORDINATOR_PORT = 9500;
    static final int NODE_COUNT = 3;

    @BeforeAll
    static void startCluster() throws Exception {
        cluster = TestCluster.start(
            NODE_COUNT,
            NODE_START_PORT,
            COORDINATOR_PORT,
            2
        );
    }

    @AfterAll
    static void stopCluster() {
        cluster.stop();
    }

    @Test
    void failsOverToReplicaWhenPrimaryDies() throws Exception {
        var putResponse = cluster.request(
            "PUT",
            GSON.toJson(new PutRequest("failover-key", "failover-value"))
        );
        String primaryId = putResponse
            .headers()
            .firstValue("X-Node-Id")
            .orElseThrow();

        cluster.killNodeProcess(primaryId);
        cluster.awaitNodeMarkedDown(primaryId);

        var getResponse = cluster.request(
            "GET",
            GSON.toJson(new GetRequest("failover-key"))
        );
        String servingNodeId = getResponse
            .headers()
            .firstValue("X-Node-Id")
            .orElseThrow();
        assertNotEquals(primaryId, servingNodeId);

        var parsed = GSON.fromJson(getResponse.body(), GetResponse.class);
        assertTrue(parsed.found());
        assertEquals("failover-value", parsed.value());
    }

    @Test
    void nodeSelfHealsAfterCrashAndRecoversData() throws Exception {
        var putResponse = cluster.request(
            "PUT",
            GSON.toJson(new PutRequest("heal-key", "heal-value"))
        );
        assertEquals(201, putResponse.statusCode());
        String nodeId = putResponse
            .headers()
            .firstValue("X-Node-Id")
            .orElseThrow();

        cluster.killNodeProcess(nodeId);
        cluster.awaitNodeMarkedDown(nodeId);
        cluster.awaitNodeHealthyAgain(nodeId);

        // check if node is still there
        var port = cluster.portForNodeId(nodeId);
        var getResponse = cluster.requestDirect(
            port,
            "GET",
            "/",
            GSON.toJson(new GetRequest("heal-key"))
        );
        var parsed = GSON.fromJson(getResponse.body(), GetResponse.class);

        assertTrue(
            parsed.found(),
            "expected restarted node to have recovered its data from the WAL"
        );
        assertEquals("heal-value", parsed.value());
    }

    @Test
    void writeReplicatesToPeers() throws Exception {
        String requestBody = GSON.toJson(
            new PutRequest("repl-key", "repl-value")
        );
        var putResponse = cluster.request("PUT", requestBody);
        assertEquals(201, putResponse.statusCode());

        String primaryNodeId = putResponse
            .headers()
            .firstValue("X-Node-Id")
            .orElseThrow();
        System.out.println("Primary node: " + primaryNodeId);

        String getBody = GSON.toJson(new GetRequest("repl-key"));

        Thread.sleep(1000); // let async replication land

        int nodesWithValue = 0;
        for (int i = 0; i < NODE_COUNT; i++) {
            int port = NODE_START_PORT + i;

            var getResponse = cluster.requestDirect(port, "GET", "/", getBody);
            System.out.printf(
                "port %d -> status=%d body=%s%n",
                port,
                getResponse.statusCode(),
                getResponse.body()
            );

            var parsed = GSON.fromJson(getResponse.body(), GetResponse.class);
            if (
                parsed != null &&
                parsed.found() &&
                "repl-value".equals(parsed.value())
            ) {
                nodesWithValue++;
            }
        }

        assertTrue(
            nodesWithValue >= 2,
            "expected replication factor 2, found on " +
                nodesWithValue +
                " node(s)"
        );
    }
}
