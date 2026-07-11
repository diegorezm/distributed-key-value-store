package src.main.java.kvcluster.cli.store;

import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import src.main.java.kvcluster.cli.ClientErrors;
import src.main.java.kvcluster.cli.services.CoordinatorClientService;

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

    @ParentCommand
    StoreCommand parent;

    @Override
    public void run() {
        CoordinatorClientService client = parent.root.client();
        var result = ClientErrors.handle(() -> client.put(key, value));
        System.out.println(
            Ansi.AUTO.string("@|green ✓|@ stored @|bold " + result.key() + "|@")
        );
    }
}
