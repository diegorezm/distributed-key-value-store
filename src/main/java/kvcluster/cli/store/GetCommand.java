package src.main.java.kvcluster.cli.store;

import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import src.main.java.kvcluster.cli.ClientErrors;
import src.main.java.kvcluster.cli.services.CoordinatorClientService;

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
        var result = ClientErrors.handle(() -> client.get(key));
        if (result.found()) {
            System.out.println(
                Ansi.AUTO.string(
                    "@|green " + key + "|@ = @|bold " + result.value() + "|@"
                )
            );
        } else {
            System.out.println(
                Ansi.AUTO.string(
                    "@|yellow (no value found for '" + key + "')|@"
                )
            );
        }
    }
}
