package cli;

import cli.node.NodeCommand;
import cli.store.StoreCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "kvctl",
    mixinStandardHelpOptions = true,
    version = "0.1.0",
    description = "CLI for interacting with KVCluster.",
    subcommands = {
        StoreCommand.class,
        NodeCommand.class
    }
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

    @Override
    public void run() {
        IO.println("Specify a subcommand. Use --help for more information.");
    }
}
