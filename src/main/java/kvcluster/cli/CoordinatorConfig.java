package src.main.java.kvcluster.cli;

public record CoordinatorConfig(
    String host,
    int port,
    int timeoutMs
) {
    public String baseUrl() {
        return "http://%s:%d".formatted(host, port);
    }
}
