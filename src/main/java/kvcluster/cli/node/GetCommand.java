package src.main.java.kvcluster.cli.node;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import src.main.java.kvcluster.cli.ClientErrors;
import src.main.java.kvcluster.cli.IO;
import src.main.java.kvcluster.cli.domain.NodeClient;
import src.main.java.kvcluster.shared.models.NodeStatus;

@Command(
    name = "get",
    description = "Show node details.",
    mixinStandardHelpOptions = true
)
public class GetCommand implements Runnable {

    @Parameters(index = "0", description = "Node ID")
    String nodeId;

    private final NodeClient client;

    public GetCommand(NodeClient client) {
        this.client = client;
    }

    @Override
    public void run() {
        NodeStatus node = ClientErrors.handle(() -> client.getNode(nodeId));
        if (node == null) {
            IO.ansi("@|yellow No node found with id '" + nodeId + "'|@");
            return;
        }
        String status = node.healthy()
            ? picocli.CommandLine.Help.Ansi.AUTO.string("@|green UP|@")
            : picocli.CommandLine.Help.Ansi.AUTO.string("@|red DOWN|@");
        IO.printf("id:      %s%n", node.id());
        IO.printf("url:     %s%n", node.url());
        IO.printf("status:  %s%n", status);
    }
}
