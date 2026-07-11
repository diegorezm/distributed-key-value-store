package src.main.java.kvcluster.cli.store;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import src.main.java.kvcluster.cli.ClientErrors;
import src.main.java.kvcluster.cli.IO;

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
        var result = ClientErrors.handle(() -> parent.client().put(key, value));
        IO.ansi("@|green \u2713|@ stored @|bold " + result.key() + "|@");
    }
}
