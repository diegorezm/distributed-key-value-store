package cli.node;

import cli.ClientErrors;
import cli.services.CoordinatorClientService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.ParentCommand;
import shared.dto.ListNodeResponseDTO;
import shared.dto.NodeStatusDTO;

@Command(
    name = "list",
    description = "List cluster nodes.",
    mixinStandardHelpOptions = true
)
public class ListCommand implements Runnable {

    @ParentCommand
    NodeCommand parent;

    @Override
    public void run() {
        CoordinatorClientService client = parent.root.client();

        ListNodeResponseDTO result = ClientErrors.handle(client::listNodes);

        for (NodeStatusDTO node : result.nodes()) {
            String status = node.healthy()
                ? Ansi.AUTO.string("@|green UP  |@")
                : Ansi.AUTO.string("@|red DOWN|@");
            System.out.printf(
                "%-10s %-25s %s%n",
                node.id(),
                node.url(),
                status
            );
        }
    }
}
