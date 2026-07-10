///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.google.code.gson:gson:2.11.0
//DEPS info.picocli:picocli:4.7.7
//SOURCES cli

import cli.KvctlCommand;
import picocli.CommandLine;

void main(String[] args) {
    int exitCode = new CommandLine(new KvctlCommand()).execute(args);
    System.exit(exitCode);
}
