package src.main.java.kvcluster.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IFactory;
import picocli.CommandLine.Option;

import src.main.java.kvcluster.cli.domain.NodeClient;
import src.main.java.kvcluster.cli.domain.StoreClient;
import src.main.java.kvcluster.cli.infra.HttpNodeClient;
import src.main.java.kvcluster.cli.infra.HttpStoreClient;
import src.main.java.kvcluster.cli.node.GetCommand;
import src.main.java.kvcluster.cli.node.ListCommand;
import src.main.java.kvcluster.cli.node.NodeCommand;
import src.main.java.kvcluster.cli.store.DelCommand;
import src.main.java.kvcluster.cli.store.PutCommand;
import src.main.java.kvcluster.cli.store.StoreCommand;

/**
 * Composition root — wires infra adapters and injects them into subcommands via IFactory.
 */
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

    @Override
    public void run() {
        IO.println("Specify a subcommand. Use --help for more information.");
    }

    /**
     * Builds a picocli IFactory that injects the right port into each command.
     * Called from the JBang entry point when constructing the CommandLine instance.
     *
     * Usage:
     *   KvctlCommand root = new KvctlCommand();
     *   new CommandLine(root, root.factory()).execute(args);
     *
     * Note: options are not yet parsed when factory() is called, so adapters must
     * be constructed lazily. We do this by capturing a reference to `this` and
     * building the config on first use.
     */
    public IFactory factory() {
        return type -> {
            CoordinatorConfig config = new CoordinatorConfig(
                coordinatorHost, coordinatorPort, timeoutMs
            );
            StoreClient store = new HttpStoreClient(config);
            NodeClient node  = new HttpNodeClient(config);

            // store subcommands
            if (type == PutCommand.class)  return new PutCommand(store);
            if (type == GetCommand.class && type.getPackageName().contains(".store")) return new src.main.java.kvcluster.cli.store.GetCommand(store);
            if (type == DelCommand.class)  return new DelCommand(store);

            // node subcommands
            if (type == ListCommand.class) return new ListCommand(node);
            if (type == GetCommand.class)  return new GetCommand(node);

            // everything else: default picocli behaviour
            return CommandLine.defaultFactory().create(type);
        };
    }
}
