package cli.store;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "get", description = "Retrieve a value.")
public class GetCommand implements Runnable {

    @Parameters(index = "0", description = "Key")
    String key;

    @Override
    public void run() {
        // TODO
    }
}
