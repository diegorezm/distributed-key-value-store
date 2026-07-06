import server.StoreNodeServer;

///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.google.code.gson:gson:2.11.0
//DEPS org.slf4j:slf4j-api:1.7.36
//DEPS org.slf4j:slf4j-simple:1.7.36
//JAVA_OPTS -Dorg.slf4j.simpleLogger.defaultLogLevel=debug
//JAVA_OPTS -Dorg.slf4j.simpleLogger.showDateTime=true
//JAVA_OPTS -Dorg.slf4j.simpleLogger.dateTimeFormat=yyyy-MM-dd HH:mm:ss
//SOURCES **/*.java

void main() {
    StoreNodeServer node = new StoreNodeServer(4000);
    node.run();
}
