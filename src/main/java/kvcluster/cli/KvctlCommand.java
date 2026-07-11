package src.main.java.kvcluster.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IFactory;
import picocli.CommandLine.Option;

import src.main.java.kvcluster.cli.domain.NodeClient;
import src.main.java.kvcluster.cli.domain.StoreClient;
import src.main.java.kvcluster.cli.infra.HttpNodeClient;
import src.main.java.kvcluster.cli.infra.HttpStoreClient;
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
     * Returns a picocli IFactory that injects the right port into each command.
     *
     * IFactory.create is a generic method (<K> K create(Class<K>)), which means
     * Java cannot use a lambda here — the compiler cannot reconcile the unchecked
     * cast from Object to K inside a lambda body. An anonymous class with an
     * explicit @SuppressWarnings("unchecked") cast is the correct solution.
     *
     * Usage from the JBang entry point:
     *   KvctlCommand root = new KvctlCommand();
     *   new CommandLine(root, root.factory()).execute(args);
     */
    public IFactory factory() {
        return new IFactory() {
            @Override
            @SuppressWarnings("unchecked")
            public <K> K create(Class<K> type) throws Exception {
                CoordinatorConfig config = new CoordinatorConfig(
                    coordinatorHost, coordinatorPort, timeoutMs
                );
                StoreClient store = new HttpStoreClient(config);
                NodeClient node   = new HttpNodeClient(config);

                // store subcommands
                if (type == PutCommand.class)
                    return (K) new PutCommand(store);
                if (type == src.main.java.kvcluster.cli.store.GetCommand.class)
                    return (K) new src.main.java.kvcluster.cli.store.GetCommand(store);
                if (type == DelCommand.class)
                    return (K) new DelCommand(store);

                // node subcommands
                if (type == ListCommand.class)
                    return (K) new ListCommand(node);
                if (type == src.main.java.kvcluster.cli.node.GetCommand.class)
                    return (K) new src.main.java.kvcluster.cli.node.GetCommand(node);

                // everything else: default picocli behaviour
                return CommandLine.defaultFactory().create(type);
            }
        };
    }
}
