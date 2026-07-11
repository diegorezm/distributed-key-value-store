package cli.node;

import cli.KvctlCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParentCommand;

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

    @ParentCommand
    KvctlCommand root;

    @Override
    public void run() {
        throw new CommandLine.ParameterException(
              spec.commandLine(),
              "Missing required subcommand."
          );
    }
}
