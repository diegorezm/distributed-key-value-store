///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.google.code.gson:gson:2.11.0
//SOURCES *.java

void main() {
    ServerStore server = new ServerStore(4000);
    server.run();
}
