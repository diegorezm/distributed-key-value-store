package src.main.java.kvcluster.node.services;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import src.main.java.kvcluster.node.dto.WalEntry;

public class WriteAheadLogService {
   private static final Gson GSON = new Gson();

    private final Path logPath;
    private BufferedWriter bw;

    public WriteAheadLogService(String nodeId) throws IOException {
        Path dataDir = Path.of("data");
        Files.createDirectories(dataDir);
        this.logPath = dataDir.resolve(nodeId + ".log");
        this.bw = new BufferedWriter(new FileWriter(logPath.toFile(), true));
    }

    /** Reads every entry currently in the log file, in order. Used at startup to replay state. */
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

    public synchronized void append(WalEntry entry) throws IOException {
        bw.write(GSON.toJson(entry));
        bw.newLine();
        bw.flush(); // force to disk
    }

    public synchronized void close() throws IOException {
        bw.close();
    }
}
