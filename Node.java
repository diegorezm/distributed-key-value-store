import server.NodeServer;

///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.google.code.gson:gson:2.11.0
//DEPS org.slf4j:slf4j-api:1.7.36
//DEPS org.slf4j:slf4j-simple:1.7.36
//JAVA_OPTS -Dorg.slf4j.simpleLogger.defaultLogLevel=debug
//JAVA_OPTS -Dorg.slf4j.simpleLogger.showDateTime=true
//JAVA_OPTS -Dorg.slf4j.simpleLogger.dateTimeFormat=yyyy-MM-dd HH:mm:ss
//SOURCES server/*.java
//SOURCES services/*.java
//SOURCES http/*.java
//SOURCES dto/*.java

void main(String[] args) {
    int port = 4000;
    String id = "node-1";

    for (String arg : args) {
        if (arg.startsWith("--port=")) {
            port = Integer.parseInt(arg.substring("--port=".length()));
        } else if (arg.startsWith("--id=")) {
            id = arg.substring("--id=".length());
        }
    }
    new NodeServer(port, id).run();
}
