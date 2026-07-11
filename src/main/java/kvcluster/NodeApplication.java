///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.google.code.gson:gson:2.11.0
//DEPS org.slf4j:slf4j-api:1.7.36
//DEPS org.slf4j:slf4j-simple:1.7.36
//DEPS info.picocli:picocli:4.7.7
//JAVA_OPTS -Dorg.slf4j.simpleLogger.defaultLogLevel=debug
//JAVA_OPTS -Dorg.slf4j.simpleLogger.showDateTime=true
//JAVA_OPTS -Dorg.slf4j.simpleLogger.dateTimeFormat=yyyy-MM-dd HH:mm:ss
//SOURCES node
//SOURCES shared

package src.main.java.kvcluster;

import picocli.CommandLine;
import src.main.java.kvcluster.node.NodeCommand;

public class NodeApplication {

    public static void main(String... args) {
        int exitCode = new CommandLine(new NodeCommand()).execute(args);
        System.exit(exitCode);
    }
}
