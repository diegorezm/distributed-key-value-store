package coordinator.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import coordinator.NodeHealthMonitor;
import coordinator.NodeInfo;
import java.io.IOException;
import java.util.List;

import shared.dto.ListNodeResponseDTO;
import shared.dto.NodeStatusDTO;
import shared.http.HttpResponseWriter;

public class ListNodesHandler implements HttpHandler {
    private final List<NodeInfo> nodes;
    private final NodeHealthMonitor healthMonitor;

    public ListNodesHandler(List<NodeInfo> nodes, NodeHealthMonitor healthMonitor) {
        this.nodes = nodes;
        this.healthMonitor = healthMonitor;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        List<NodeStatusDTO> statuses = nodes.stream()
               .map(n -> new NodeStatusDTO(
                   n.id(),
                   "http://localhost:" + n.port(),
                   healthMonitor.isHealthy(n.id())
               ))
               .toList();
        HttpResponseWriter.send(exchange, 200, new ListNodeResponseDTO(statuses));
    }
}
