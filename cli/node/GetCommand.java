package cli.node;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "get", description = "Show node details.")
public class GetCommand implements Runnable {

    @Parameters(index = "0", description = "Node ID")
    String nodeId;

    @Override
    public void run() {
        // TODO
    }
}
