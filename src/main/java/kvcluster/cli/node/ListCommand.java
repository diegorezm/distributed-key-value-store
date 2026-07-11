package src.main.java.kvcluster.cli.node;

import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;
import src.main.java.kvcluster.cli.ClientErrors;
import src.main.java.kvcluster.cli.IO;
import src.main.java.kvcluster.shared.models.NodeStatus;

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
        var nodes = ClientErrors.handle(parent.client()::listNodes);
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
