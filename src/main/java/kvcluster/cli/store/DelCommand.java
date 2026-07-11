package src.main.java.kvcluster.cli.store;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import src.main.java.kvcluster.cli.ClientErrors;
import src.main.java.kvcluster.cli.IO;

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
        var result = ClientErrors.handle(() -> parent.client().delete(key));
        IO.ansi("@|green \u2713|@ deleted @|bold " + result.key() + "|@");
    }
}
