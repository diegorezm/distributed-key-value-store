package src.main.java.kvcluster.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import src.main.java.kvcluster.cli.node.NodeCommand;
import src.main.java.kvcluster.cli.services.CoordinatorClientService;
import src.main.java.kvcluster.cli.store.StoreCommand;

@Command(
    name = "kvctl",
    mixinStandardHelpOptions = true,
    version = "0.1.0",
    description = "CLI for interacting with KVCluster.",
    subcommands = { StoreCommand.class, NodeCommand.class }
)
public class KvctlCommand implements Runnable {

    @Option(
        names = { "-H", "--host" },
        description = "Coordinator host.",
        defaultValue = "localhost"
    )
    String coordinatorHost;

    @Option(
        names = { "-p", "--port" },
        description = "Coordinator port.",
        defaultValue = "9000"
    )
    int coordinatorPort;

    @Option(
        names = { "-t", "--timeout-ms" },
        description = "Request timeout in milliseconds.",
        defaultValue = "3000"
    )
    int timeoutMs;

    private CoordinatorClientService client;

    public CoordinatorClientService client() {
        if (client == null) {
            client = new CoordinatorClientService(
                coordinatorHost,
                coordinatorPort,
                timeoutMs
            );
        }
        return client;
    }

    @Override
    public void run() {
        IO.println("Specify a subcommand. Use --help for more information.");
    }
}
