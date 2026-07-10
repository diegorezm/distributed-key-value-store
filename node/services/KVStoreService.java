package node.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import node.dto.WalEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KVStoreService {

    private static final Logger logger = LoggerFactory.getLogger(
        KVStoreService.class
    );
    private final WriteAheadLogService wal;

    private HashMap<String, String> kv;

    public KVStoreService(String nodeId) throws IOException {
        this.kv = new HashMap<>();
        this.wal = new WriteAheadLogService(nodeId);
        this.restore();
    }

    public void put(String key, String val) {
        try {
            wal.append(WalEntry.put(key, val));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to WAL", e);
        }
        this.kv.put(key, val);
    }

    public void del(String key) {
        try {
            wal.append(WalEntry.delete(key));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to WAL", e);
        }
        this.kv.remove(key);
    }

    public String get(String key) {
        return this.kv.get(key);
    }

    private void restore() throws IOException {
        List<WalEntry> entries = wal.readAll();
        for (WalEntry entry : entries) {
            switch (entry.op()) {
                case "PUT" -> kv.put(entry.key(), entry.value());
                case "DELETE" -> kv.remove(entry.key());
            }
        }
        logger.info(
            "Replayed {} WAL entries, {} keys restored",
            entries.size(),
            kv.size()
        );
    }

}
