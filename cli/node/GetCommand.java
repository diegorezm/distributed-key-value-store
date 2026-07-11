package cli.node;

import cli.ClientErrors;
import cli.services.CoordinatorClientService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import shared.dto.NodeStatusDTO;

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

        NodeStatusDTO node = ClientErrors.handle(() -> client.getNode(nodeId));

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
