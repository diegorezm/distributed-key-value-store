package src.main.java.kvcluster.cli;

import picocli.CommandLine.Help.Ansi;

/**
 * Thin wrapper around stdout/stderr so commands never call System.out directly.
 */
public final class IO {
    private IO() {}

    public static void println(String message) {
        System.out.println(message);
    }

    public static void ansi(String markup) {
        System.out.println(Ansi.AUTO.string(markup));
    }

    public static void err(String message) {
        System.err.println(Ansi.AUTO.string("@|red " + message + "|@"));
    }

    public static void printf(String format, Object... args) {
        System.out.printf(format, args);
    }
}
