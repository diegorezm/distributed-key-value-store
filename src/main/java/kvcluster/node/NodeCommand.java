package src.main.java.kvcluster.node;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "node", mixinStandardHelpOptions = true)
public class NodeCommand implements Callable<Integer> {

    @Option(names = {"--port", "-p"}, defaultValue = "4000")
    int port;

    @Option(names = "--id", defaultValue = "node-1")
    String id;

    @Option(
        names = "--peers",
        split = ",",
        description = "Comma-separated peers in the format id=host:port,id2=host:port"
    )
    Map<String, String> peers = new LinkedHashMap<>();

    @Override
    public Integer call() throws Exception {
        var node = new NodeServer(port, id, peers);
        node.run();
        return 0;
    }
}
