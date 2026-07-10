package cli.store;

import cli.services.CoordinatorClientService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(
    name = "get",
    description = "Retrieve a value.",
    mixinStandardHelpOptions = true
)
public class GetCommand implements Runnable {

    @Parameters(index = "0", description = "Key")
    String key;

    @ParentCommand
    StoreCommand parent;

    @Override
    public void run() {
        CoordinatorClientService client = parent.root.client();
        client.get(key);
    }
}
