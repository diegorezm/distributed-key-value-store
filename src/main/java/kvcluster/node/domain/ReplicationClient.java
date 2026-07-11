package src.main.java.kvcluster.node.domain;

/**
 * PORT — defines how the domain replicates writes to peer nodes.
 * Implementations live in infra/.
 */
public interface ReplicationClient {
    void replicate(String method, String body);
}
