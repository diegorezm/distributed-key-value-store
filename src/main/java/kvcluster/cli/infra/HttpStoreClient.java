package src.main.java.kvcluster.cli.infra;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.google.gson.Gson;

import src.main.java.kvcluster.cli.CoordinatorConfig;
import src.main.java.kvcluster.cli.domain.StoreClient;
import src.main.java.kvcluster.shared.models.DelRequest;
import src.main.java.kvcluster.shared.models.DeleteResponse;
import src.main.java.kvcluster.shared.models.GetRequest;
import src.main.java.kvcluster.shared.models.GetResponse;
import src.main.java.kvcluster.shared.models.PutRequest;
import src.main.java.kvcluster.shared.models.PutResponse;

/**
 * ADAPTER — implements StoreClient by sending HTTP requests to the coordinator.
 */
public class HttpStoreClient implements StoreClient {

    private static final Gson GSON = new Gson();

    private final String baseUrl;
    private final HttpClient http;

    public HttpStoreClient(CoordinatorConfig config) {
        this.baseUrl = config.baseUrl();
        this.http = HttpClient.newBuilder()
            .followRedirects(Redirect.NORMAL)
            .connectTimeout(Duration.ofMillis(config.timeoutMs()))
            .build();
    }

    @Override
    public PutResponse put(String key, String value) throws IOException, InterruptedException {
        String body = GSON.toJson(new PutRequest(key, value));
        HttpResponse<String> response = send("PUT", "/", body);
        return GSON.fromJson(response.body(), PutResponse.class);
    }

    @Override
    public GetResponse get(String key) throws IOException, InterruptedException {
        String body = GSON.toJson(new GetRequest(key));
        HttpResponse<String> response = send("GET", "/", body);
        return GSON.fromJson(response.body(), GetResponse.class);
    }

    @Override
    public DeleteResponse delete(String key) throws IOException, InterruptedException {
        String body = GSON.toJson(new DelRequest(key));
        HttpResponse<String> response = send("DELETE", "/", body);
        return GSON.fromJson(response.body(), DeleteResponse.class);
    }

    private HttpResponse<String> send(String method, String path, String jsonBody)
            throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .header("Content-Type", "application/json")
            .method(method, HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }
}
