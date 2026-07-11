package src.main.java.kvcluster.coordinator;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "coordinator", mixinStandardHelpOptions = true)
public class CoordinatorCommand implements Callable<Integer> {

    @Option(names = "--node-jar", description = "Path to node launcher script")
    private Path nodeJarPath;

    @Option(names = "--nodes", defaultValue = "3")
    int nodeCount;

    @Option(names = "--port", defaultValue = "4000")
    int startingNodePort;

    @Option(names = "--coordinator-port", defaultValue = "9000")
    int coordinatorPort;

    @Option(names = "--replication", defaultValue = "2")
    int replicationFactor;

    @Override
    public Integer call() throws Exception {
        CoordinatorServer coordinator = new CoordinatorServer(nodeJarPath);
        coordinator.run(
            nodeCount,
            startingNodePort,
            coordinatorPort,
            replicationFactor
        );
        return 0;
    }
}
