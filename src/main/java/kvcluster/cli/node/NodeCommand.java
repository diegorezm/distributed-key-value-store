package src.main.java.kvcluster.cli.node;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(
    name = "node",
    description = "Manage cluster nodes.",
    mixinStandardHelpOptions = true,
    subcommands = {
        ListCommand.class,
        GetCommand.class
    }
)
public class NodeCommand implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        throw new CommandLine.ParameterException(
            spec.commandLine(),
            "Missing required subcommand."
        );
    }
}
