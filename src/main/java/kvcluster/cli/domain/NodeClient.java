package src.main.java.kvcluster.cli.domain;

import java.io.IOException;
import java.util.List;

import src.main.java.kvcluster.shared.models.NodeStatus;

/**
 * PORT — what the node commands need from the cluster.
 * Implementations live in infra/.
 */
public interface NodeClient {
    List<NodeStatus> listNodes() throws IOException, InterruptedException;
    NodeStatus getNode(String id) throws IOException, InterruptedException;
}
