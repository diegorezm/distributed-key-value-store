package cli.services;

import java.net.http.HttpClient;
import java.time.Duration;

public class CoordinatorClientService {

    private final HttpClient httpClient;
    private final String baseUrl;

    public CoordinatorClientService(String host, int port, int timeoutMs) {
        this.baseUrl = "http://%s:%d".formatted(host, port);

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    public String baseUrl() {
        return baseUrl;
    }

    // TODO: this
    public void put(String key, String value) {}

    public void get(String key) {}

    public void delete(String key) {}

    public void listNodes() {}

    public void getNode(String id) {}
}
