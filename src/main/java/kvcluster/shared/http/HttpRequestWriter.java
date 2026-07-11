package src.main.java.kvcluster.shared.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.Gson;

public final class HttpRequestWriter {

    private static final Gson GSON = new Gson();
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private HttpRequestWriter() {}

    public static HttpResponse<String> post(String url, Object body)
            throws IOException, InterruptedException {

        String json = GSON.toJson(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
