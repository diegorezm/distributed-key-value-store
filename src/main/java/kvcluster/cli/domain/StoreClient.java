package src.main.java.kvcluster.cli.domain;

import java.io.IOException;

import src.main.java.kvcluster.shared.models.DeleteResponse;
import src.main.java.kvcluster.shared.models.GetResponse;
import src.main.java.kvcluster.shared.models.PutResponse;

/**
 * PORT — what the store commands need from the cluster.
 * Implementations live in infra/.
 */
public interface StoreClient {
    PutResponse put(String key, String value) throws IOException, InterruptedException;
    GetResponse get(String key) throws IOException, InterruptedException;
    DeleteResponse delete(String key) throws IOException, InterruptedException;
}
