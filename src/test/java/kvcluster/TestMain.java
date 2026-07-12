///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.junit.platform:junit-platform-launcher:6.0.0
//DEPS org.junit.platform:junit-platform-engine:6.0.0
//DEPS org.junit.jupiter:junit-jupiter-engine:6.0.0
//DEPS com.google.code.gson:gson:2.11.0
//DEPS org.slf4j:slf4j-api:1.7.36
//DEPS org.slf4j:slf4j-simple:1.7.36
//DEPS info.picocli:picocli:4.7.7
//JAVA_OPTS -Dorg.slf4j.simpleLogger.defaultLogLevel=off
//SOURCES ../../../main/java/kvcluster/**/*.java
//SOURCES coordinator
//SOURCES integration

package src.test.java.kvcluster;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

import java.io.PrintWriter;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

public class TestMain {
    public static void main(String[] args) {
        var request = LauncherDiscoveryRequestBuilder.request()
            .selectors(selectPackage("src.test.java.kvcluster"))
            .build();

        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.execute(request, listener);

        PrintWriter out = new PrintWriter(System.out);
        listener.getSummary().printFailuresTo(out, 1024);
        listener.getSummary().printTo(out);
        out.flush();

        if (listener.getSummary().getTotalFailureCount() > 0) {
            System.exit(1);
        }
    }
}
