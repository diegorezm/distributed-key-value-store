///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.google.code.gson:gson:2.11.0
//DEPS org.slf4j:slf4j-api:1.7.36
//DEPS org.slf4j:slf4j-simple:1.7.36
//JAVA_OPTS -Dorg.slf4j.simpleLogger.defaultLogLevel=debug
//JAVA_OPTS -Dorg.slf4j.simpleLogger.showDateTime=true
//JAVA_OPTS -Dorg.slf4j.simpleLogger.dateTimeFormat=yyyy-MM-dd HH:mm:ss
//SOURCES coordinator
//SOURCES shared

import coordinator.CoordinatorServer;

void main(String[] args) throws Exception {
    int nodeCount = 3;
    int startingNodePort = 4000;
    int coordinatorPort = 9000;
    int replicationFactor = 2;

    for (String arg : args) {
        if (arg.startsWith("--nodes=")) {
            nodeCount = Integer.parseInt(arg.substring("--nodes=".length()));
        } else if (arg.startsWith("--port=")) {
            startingNodePort = Integer.parseInt(arg.substring("--port=".length()));
        } else if (arg.startsWith("--coordinator-port=")) {
            coordinatorPort = Integer.parseInt(arg.substring("--coordinator-port=".length()));
        } else if (arg.startsWith("--replication=")) {
            replicationFactor = Integer.parseInt(arg.substring("--replication=".length()));
        }
    }

    CoordinatorServer coordinator = new CoordinatorServer();
    coordinator.run(nodeCount, startingNodePort, coordinatorPort, replicationFactor);
}
