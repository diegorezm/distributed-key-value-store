package coordinator.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import coordinator.NodeInfo;
import java.io.IOException;
import java.util.List;
import shared.http.HttpResponseWriter;

public class ListNodesHandler implements HttpHandler {
    private List<NodeInfo> nodes;

    public ListNodesHandler(List<NodeInfo> nodes) {
        this.nodes = nodes;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpResponseWriter.send(exchange, 200, this.nodes);
    }
}
