///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.google.code.gson:gson:2.11.0
//DEPS org.slf4j:slf4j-api:1.7.36
//DEPS org.slf4j:slf4j-simple:1.7.36
//JAVA_OPTS -Dorg.slf4j.simpleLogger.defaultLogLevel=debug
//JAVA_OPTS -Dorg.slf4j.simpleLogger.showDateTime=true
//JAVA_OPTS -Dorg.slf4j.simpleLogger.dateTimeFormat=yyyy-MM-dd HH:mm:ss
//SOURCES node
//SOURCES shared

import node.NodeServer;

void main(String[] args) {
    int port = 4000;
    String id = "node-1";
    Map<String, String> peers = new HashMap<>();

    for (String arg : args) {
        if (arg.startsWith("--port=")) {
            port = Integer.parseInt(arg.substring("--port=".length()));
        } else if (arg.startsWith("--id=")) {
            id = arg.substring("--id=".length());
        } else if (arg.startsWith("--peers=")) {
            String raw = arg.substring("--peers=".length());
            if (!raw.isBlank()) {
                for (String pair : raw.split(",")) {
                    String[] kv = pair.split("=", 2);
                    peers.put(kv[0], kv[1]);
                }
            }
        }
    }
    new NodeServer(port, id, peers).run();
}
