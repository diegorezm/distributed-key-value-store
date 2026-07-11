package src.main.java.kvcluster.node.infra;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import src.main.java.kvcluster.node.domain.KVService;
import src.main.java.kvcluster.node.domain.WalWriter;
import src.main.java.kvcluster.node.domain.model.WalEntry;

/**
 * ADAPTER — implements KVService backed by an in-memory HashMap and a WalWriter.
 */
public class DefaultKVService implements KVService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultKVService.class);

    private final Map<String, String> store = new HashMap<>();
    private final WalWriter wal;

    public DefaultKVService(WalWriter wal) throws IOException {
        this.wal = wal;
        restore();
    }

    @Override
    public void put(String key, String value) {
        try {
            wal.append(WalEntry.put(key, value));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to WAL", e);
        }
        store.put(key, value);
    }

    @Override
    public void del(String key) {
        try {
            wal.append(WalEntry.delete(key));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to WAL", e);
        }
        store.remove(key);
    }

    @Override
    public String get(String key) {
        return store.get(key);
    }

    private void restore() throws IOException {
        List<WalEntry> entries = wal.readAll();
        for (WalEntry entry : entries) {
            switch (entry.op()) {
                case "PUT" -> store.put(entry.key(), entry.value());
                case "DELETE" -> store.remove(entry.key());
            }
        }
        logger.info("Replayed {} WAL entries, {} keys restored", entries.size(), store.size());
    }
}
