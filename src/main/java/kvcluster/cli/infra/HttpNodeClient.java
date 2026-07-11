package src.main.java.kvcluster.cli.infra;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import com.google.gson.Gson;

import src.main.java.kvcluster.cli.CoordinatorConfig;
import src.main.java.kvcluster.cli.domain.NodeClient;
import src.main.java.kvcluster.shared.models.ListNodeResponse;
import src.main.java.kvcluster.shared.models.NodeStatus;

/**
 * ADAPTER — implements NodeClient by sending HTTP requests to the coordinator.
 */
public class HttpNodeClient implements NodeClient {

    private static final Gson GSON = new Gson();

    private final String baseUrl;
    private final HttpClient http;

    public HttpNodeClient(CoordinatorConfig config) {
        this.baseUrl = config.baseUrl();
        this.http = HttpClient.newBuilder()
            .followRedirects(Redirect.NORMAL)
            .connectTimeout(Duration.ofMillis(config.timeoutMs()))
            .build();
    }

    @Override
    public List<NodeStatus> listNodes() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/nodes"))
            .header("Content-Type", "application/json")
            .GET()
            .build();
        HttpResponse<String> response = http.send(req, HttpResponse.BodyHandlers.ofString());
        return GSON.fromJson(response.body(), ListNodeResponse.class).nodes();
    }

    @Override
    public NodeStatus getNode(String id) throws IOException, InterruptedException {
        return listNodes().stream()
            .filter(n -> n.id().equals(id))
            .findFirst()
            .orElse(null);
    }
}
