package src.main.java.kvcluster.node.infra;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import src.main.java.kvcluster.node.domain.WalWriter;
import src.main.java.kvcluster.node.domain.model.WalEntry;

/**
 * ADAPTER — implements WalWriter by appending JSON lines to a per-node file under data/.
 */
public class FileWalWriter implements WalWriter {

    private static final Gson GSON = new Gson();

    private final Path logPath;
    private final BufferedWriter bw;

    public FileWalWriter(String nodeId) throws IOException {
        Path dataDir = Path.of("data");
        Files.createDirectories(dataDir);
        this.logPath = dataDir.resolve(nodeId + ".log");
        this.bw = new BufferedWriter(new FileWriter(logPath.toFile(), true));
    }

    @Override
    public List<WalEntry> readAll() throws IOException {
        if (!Files.exists(logPath)) {
            return List.of();
        }
        List<WalEntry> entries = new ArrayList<>();
        for (String line : Files.readAllLines(logPath)) {
            if (!line.isBlank()) {
                entries.add(GSON.fromJson(line, WalEntry.class));
            }
        }
        return entries;
    }

    @Override
    public synchronized void append(WalEntry entry) throws IOException {
        bw.write(GSON.toJson(entry));
        bw.newLine();
        bw.flush();
    }

    public synchronized void close() throws IOException {
        bw.close();
    }
}
