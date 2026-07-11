package src.main.java.kvcluster.cli.store;

import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import src.main.java.kvcluster.cli.ClientErrors;
import src.main.java.kvcluster.cli.services.CoordinatorClientService;

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
