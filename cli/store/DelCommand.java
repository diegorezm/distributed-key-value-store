package cli.store;

import cli.ClientErrors;
import cli.services.CoordinatorClientService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(
    name = "del",
    description = "Delete a key.",
    mixinStandardHelpOptions = true
)
public class DelCommand implements Runnable {

    @Parameters(index = "0", description = "Key")
    String key;

    @ParentCommand
    StoreCommand parent;

    @Override
    public void run() {
        CoordinatorClientService client = parent.root.client();
        var result = ClientErrors.handle(() -> client.delete(key));
        System.out.println(
            Ansi.AUTO.string(
                "@|green ✓|@ deleted @|bold " + result.key() + "|@"
            )
        );
    }
}
