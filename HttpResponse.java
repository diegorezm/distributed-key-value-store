import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;

class HttpResponse {

    public static void send(
        HttpExchange exchange,
        int status,
        Object responseObject
    ) throws IOException {
        Gson gson = new Gson();
        String response = gson.toJson(responseObject);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    public static void send(HttpExchange exchange, String message)
        throws IOException {
        GenericResponse gr = new GenericResponse(message);
        HttpResponse.send(exchange, 200, gr);
    }
}
