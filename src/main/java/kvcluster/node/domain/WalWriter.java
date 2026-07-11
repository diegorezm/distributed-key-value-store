package src.main.java.kvcluster.node.domain;

import java.io.IOException;
import java.util.List;

import src.main.java.kvcluster.node.domain.model.WalEntry;

/**
 * PORT — defines what the domain needs to persist and replay WAL entries.
 * Implementations live in infra/.
 */
public interface WalWriter {
    void append(WalEntry entry) throws IOException;
    List<WalEntry> readAll() throws IOException;
}
