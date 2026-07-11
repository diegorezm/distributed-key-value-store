package src.main.java.kvcluster.cli.services;

import com.google.gson.Gson;

import src.main.java.kvcluster.shared.models.DelRequest;
import src.main.java.kvcluster.shared.models.DeleteResponse;
import src.main.java.kvcluster.shared.models.GetRequest;
import src.main.java.kvcluster.shared.models.GetResponse;
import src.main.java.kvcluster.shared.models.ListNodeResponse;
import src.main.java.kvcluster.shared.models.NodeStatus;
import src.main.java.kvcluster.shared.models.PutRequest;
import src.main.java.kvcluster.shared.models.PutResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.time.Duration;

public class CoordinatorClientService {
    private static final Gson GSON = new Gson();

    private final HttpClient httpClient;
    private final String baseUrl;

    public CoordinatorClientService(String host, int port, int timeoutMs) {
        this.baseUrl = "http://%s:%d".formatted(host, port);
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(Redirect.NORMAL)
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    public String baseUrl() {
        return baseUrl;
    }

    public PutResponse put(String key, String value) throws IOException, InterruptedException {
        String body = GSON.toJson(new PutRequest(key, value));
        HttpResponse<String> response = send("PUT", "/", body);
        return GSON.fromJson(response.body(), PutResponse.class);
    }

    public GetResponse get(String key) throws IOException, InterruptedException {
        String body = GSON.toJson(new GetRequest(key));
        HttpResponse<String> response = send("GET", "/", body);
        return GSON.fromJson(response.body(), GetResponse.class);
    }

    public DeleteResponse delete(String key) throws IOException, InterruptedException {
        String body = GSON.toJson(new DelRequest(key));
        HttpResponse<String> response = send("DELETE", "/", body);
        return GSON.fromJson(response.body(), DeleteResponse.class);
    }

    public ListNodeResponse listNodes() throws IOException, InterruptedException {
        HttpResponse<String> response = send("GET", "/nodes", null); // no body needed
        return GSON.fromJson(response.body(), ListNodeResponse.class);
    }

    public NodeStatus getNode(String id) throws IOException, InterruptedException {
        return listNodes().nodes().stream()
                .filter(n -> n.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Sends a request to {@code path}. If {@code jsonBody} is null, no request body is attached
     * (used for simple GET-style calls like /nodes). Otherwise, the body is sent as-is —
     * callers are responsible for pre-serializing via GSON before calling this.
     */
    private HttpResponse<String> send(String method, String path, String jsonBody)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json");

        if (jsonBody != null) {
            builder.method(method, HttpRequest.BodyPublishers.ofString(jsonBody));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        HttpRequest request = builder.build();
        var response =  httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response;
    }
}
