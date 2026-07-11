package src.main.java.kvcluster.cli.node;

import picocli.CommandLine.Command;
import src.main.java.kvcluster.cli.ClientErrors;
import src.main.java.kvcluster.cli.IO;
import src.main.java.kvcluster.cli.domain.NodeClient;
import src.main.java.kvcluster.shared.models.NodeStatus;

@Command(
    name = "list",
    description = "List cluster nodes.",
    mixinStandardHelpOptions = true
)
public class ListCommand implements Runnable {

    private final NodeClient client;

    public ListCommand(NodeClient client) {
        this.client = client;
    }

    @Override
    public void run() {
        var nodes = ClientErrors.handle(client::listNodes);
        for (NodeStatus node : nodes) {
            String status = node.healthy()
                ? "@|green UP  |@"
                : "@|red DOWN|@";
            IO.printf("%-10s %-25s %s%n",
                node.id(),
                node.url(),
                picocli.CommandLine.Help.Ansi.AUTO.string(status));
        }
    }
}
