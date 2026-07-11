package src.main.java.kvcluster.cli.node;

import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import src.main.java.kvcluster.cli.ClientErrors;
import src.main.java.kvcluster.cli.services.CoordinatorClientService;
import src.main.java.kvcluster.shared.models.NodeStatus;

@Command(
    name = "get",
    description = "Show node details.",
    mixinStandardHelpOptions = true
)
public class GetCommand implements Runnable {

    @Parameters(index = "0", description = "Node ID")
    String nodeId;

    @ParentCommand
    NodeCommand parent;

    @Override
    public void run() {
        CoordinatorClientService client = parent.root.client();

        NodeStatus node = ClientErrors.handle(() -> client.getNode(nodeId));

        if (node == null) {
            System.out.println(
                Ansi.AUTO.string(
                    "@|yellow No node found with id '" + nodeId + "'|@"
                )
            );
            return;
        }

        String status = node.healthy()
            ? Ansi.AUTO.string("@|green UP|@")
            : Ansi.AUTO.string("@|red DOWN|@");

        System.out.printf("id:      %s%n", node.id());
        System.out.printf("url:     %s%n", node.url());
        System.out.printf("status:  %s%n", status);
    }
}
