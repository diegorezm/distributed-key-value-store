package cli.store;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "put", description = "Store a key/value pair.")
public class PutCommand implements Runnable {

    @Parameters(index = "0", description = "Key")
    String key;

    @Parameters(index = "1", description = "Value")
    String value;

    @Override
    public void run() {
        // TODO
    }
}
