package src.main.java.kvcluster.shared.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import src.main.java.kvcluster.shared.dto.GenericResponseDTO;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class HttpResponseWriter {

    private static final Gson GSON = new Gson();

    private HttpResponseWriter() {}

    public static void send(HttpExchange exchange, int status, Object body)
        throws IOException {
        String json = GSON.toJson(body);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange
            .getResponseHeaders()
            .set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void send(HttpExchange exchange, int status, String message)
        throws IOException {
        send(exchange, status, new GenericResponseDTO(message));
    }
}
