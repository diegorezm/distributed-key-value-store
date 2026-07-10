package cli.node;

import picocli.CommandLine.Command;

@Command(
    name = "list",
    description = "List cluster nodes.",
    mixinStandardHelpOptions = true
)
public class ListCommand implements Runnable {

    @Override
    public void run() {
        // TODO
    }
}
