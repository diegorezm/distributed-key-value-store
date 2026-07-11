package src.main.java.kvcluster.cli.store;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import src.main.java.kvcluster.cli.ClientErrors;
import src.main.java.kvcluster.cli.IO;

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
        var result = ClientErrors.handle(() -> parent.client().get(key));
        if (result.found()) {
            IO.ansi("@|green " + key + "|@ = @|bold " + result.value() + "|@");
        } else {
            IO.ansi("@|yellow (no value found for '" + key + "')|@");
        }
    }
}
