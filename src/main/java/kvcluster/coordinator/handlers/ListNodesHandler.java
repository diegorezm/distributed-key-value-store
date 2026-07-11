package src.main.java.kvcluster.coordinator.handlers;

import java.io.IOException;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import src.main.java.kvcluster.coordinator.NodeHealthMonitor;
import src.main.java.kvcluster.coordinator.NodeInfo;
import src.main.java.kvcluster.shared.models.ListNodeResponse;
import src.main.java.kvcluster.shared.models.NodeStatus;
import src.main.java.kvcluster.shared.http.HttpResponseWriter;

public class ListNodesHandler implements HttpHandler {
    private final List<NodeInfo> nodes;
    private final NodeHealthMonitor healthMonitor;

    public ListNodesHandler(List<NodeInfo> nodes, NodeHealthMonitor healthMonitor) {
        this.nodes = nodes;
        this.healthMonitor = healthMonitor;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        List<NodeStatus> statuses = nodes.stream()
               .map(n -> new NodeStatus(
                   n.id(),
                   "http://localhost:" + n.port(),
                   healthMonitor.isHealthy(n.id())
               ))
               .toList();
        HttpResponseWriter.send(exchange, 200, new ListNodeResponse(statuses));
    }
}
