package src.main.java.kvcluster.cli.store;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import src.main.java.kvcluster.cli.ClientErrors;
import src.main.java.kvcluster.cli.IO;
import src.main.java.kvcluster.cli.domain.StoreClient;

@Command(
    name = "del",
    description = "Delete a key.",
    mixinStandardHelpOptions = true
)
public class DelCommand implements Runnable {

    @Parameters(index = "0", description = "Key")
    String key;

    private final StoreClient client;

    public DelCommand(StoreClient client) {
        this.client = client;
    }

    @Override
    public void run() {
        var result = ClientErrors.handle(() -> client.delete(key));
        IO.ansi("@|green ✓|@ deleted @|bold " + result.key() + "|@");
    }
}
