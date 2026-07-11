package src.main.java.kvcluster.cli.store;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import src.main.java.kvcluster.cli.ClientErrors;
import src.main.java.kvcluster.cli.IO;
import src.main.java.kvcluster.cli.domain.StoreClient;

@Command(
    name = "put",
    description = "Store a key/value pair.",
    mixinStandardHelpOptions = true
)
public class PutCommand implements Runnable {

    @Parameters(index = "0", description = "Key")
    String key;

    @Parameters(index = "1", description = "Value")
    String value;

    private final StoreClient client;

    public PutCommand(StoreClient client) {
        this.client = client;
    }

    @Override
    public void run() {
        var result = ClientErrors.handle(() -> client.put(key, value));
        IO.ansi("@|green ✓|@ stored @|bold " + result.key() + "|@");
    }
}
