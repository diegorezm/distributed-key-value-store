package cli;

import java.io.IOException;
import java.net.http.HttpTimeoutException;
import picocli.CommandLine.Help.Ansi;

public final class ClientErrors {
    private ClientErrors() {}

    public interface ThrowingSupplier<T> {
        T get() throws IOException, InterruptedException;
    }

    public static <T> T handle(ThrowingSupplier<T> action) {
        try {
            return action.get();
        } catch (HttpTimeoutException e) {
            System.err.println(Ansi.AUTO.string(
                "@|red Timed out reaching the cluster.|@"
            ));
            System.exit(1);
        } catch (IOException e) {
            System.err.println(Ansi.AUTO.string(
                "@|red Could not reach the coordinator: " + e.getMessage() + "|@"
            ));
            System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println(Ansi.AUTO.string("@|red Request interrupted.|@"));
            System.exit(1);
        }
        return null; // unreachable, System.exit already terminated the process
    }
}
