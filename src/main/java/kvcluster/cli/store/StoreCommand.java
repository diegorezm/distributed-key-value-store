package src.main.java.kvcluster.cli.store;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;
import src.main.java.kvcluster.cli.KvctlCommand;
import src.main.java.kvcluster.cli.domain.StoreClient;

@Command(
    name = "store",
    description = "Manage key/value pairs.",
    mixinStandardHelpOptions = true,
    subcommands = { PutCommand.class, GetCommand.class, DelCommand.class }
)
public class StoreCommand implements Runnable {

    @Spec
    CommandSpec spec;

    @ParentCommand
    KvctlCommand root;

    public StoreClient client() {
        return root.storeClient();
    }

    @Override
    public void run() {
        throw new CommandLine.ParameterException(
            spec.commandLine(),
            "Missing required subcommand."
        );
    }
}
