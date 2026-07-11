package src.main.java.kvcluster.cli.store;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import src.main.java.kvcluster.cli.ClientErrors;
import src.main.java.kvcluster.cli.IO;
import src.main.java.kvcluster.cli.domain.StoreClient;

@Command(
    name = "get",
    description = "Retrieve a value.",
    mixinStandardHelpOptions = true
)
public class GetCommand implements Runnable {

    @Parameters(index = "0", description = "Key")
    String key;

    private final StoreClient client;

    public GetCommand(StoreClient client) {
        this.client = client;
    }

    @Override
    public void run() {
        var result = ClientErrors.handle(() -> client.get(key));
        if (result.found()) {
            IO.ansi("@|green " + key + "|@ = @|bold " + result.value() + "|@");
        } else {
            IO.ansi("@|yellow (no value found for '" + key + "')|@");
        }
    }
}
