package cli.store;

import cli.ClientErrors;
import cli.services.CoordinatorClientService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

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
