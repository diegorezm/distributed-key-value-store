package cli.store;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "del", description = "Delete a key.")
public class DelCommand implements Runnable {

    @Parameters(index = "0", description = "Key")
    String key;

    @Override
    public void run() {
        // TODO
    }
}
