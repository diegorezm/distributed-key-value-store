package cli.node;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

@Command(
    name = "node",
    description = "Manage cluster nodes.",
    subcommandsRepeatable = false,
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
