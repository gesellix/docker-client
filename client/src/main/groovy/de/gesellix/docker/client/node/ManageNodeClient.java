package de.gesellix.docker.client.node;

import de.gesellix.docker.client.EngineResponseContent;
import de.gesellix.docker.client.tasks.ManageTask;
import de.gesellix.docker.remote.api.EngineApiClient;
import de.gesellix.docker.remote.api.Node;
import de.gesellix.docker.remote.api.NodeSpec;
import de.gesellix.docker.remote.api.Task;
import de.gesellix.util.QueryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageNodeClient implements ManageNode {

  private final Logger log = LoggerFactory.getLogger(ManageNodeClient.class);

  private final EngineApiClient client;
  private final ManageTask manageTask;
  private final NodeUtil nodeUtil;

  public ManageNodeClient(EngineApiClient client, ManageTask manageTask, NodeUtil nodeUtil) {
    this.client = client;
    this.manageTask = manageTask;
    this.nodeUtil = nodeUtil;
  }

  /**
   * @see #nodes(String)
   * @deprecated use {@link #nodes(String)}
   */
  @Deprecated
  @Override
  public EngineResponseContent<List<Node>> nodes(Map<String, Object> query) {
    Map<String, Object> actualQuery = new HashMap<>();
    if (query != null) {
      actualQuery.putAll(query);
    }
    new QueryUtil().jsonEncodeQueryParameter(actualQuery, "filters");
    return nodes((String) actualQuery.get("filters"));
  }

  @Override
  public EngineResponseContent<List<Node>> nodes(String filters) {
    log.info("docker node ls");
    List<Node> response = client.getNodeApi().nodeList(filters);
    return new EngineResponseContent<>(response);
  }

  @Override
  public EngineResponseContent<List<Node>> nodes() {
    return nodes((String) null);
  }

  @Override
  public EngineResponseContent<Node> inspectNode(String name) {
    log.info("docker node inspect");
    Node nodeInspect = client.getNodeApi().nodeInspect(name);
    return new EngineResponseContent<>(nodeInspect);
  }

  @Override
  public void rmNode(String name) {
    log.info("docker node rm");
    client.getNodeApi().nodeDelete(name, null);
  }

  @Override
  public void updateNode(String name, long version, NodeSpec nodeSpec) {
    log.info("docker node update");
    client.getNodeApi().nodeUpdate(name, version, nodeSpec);
  }

  @Override
  public void promoteNodes(String... nodes) {
    log.info("docker node promote");
    for (String node : nodes) {
      Node nodeInfo = inspectNode(node).getContent();
      if (NodeSpec.Role.Manager.equals(nodeInfo.getSpec().getRole())) {
        log.warn("Node {} is already a manager.", node);
      } else {
        NodeSpec nodeSpec = new NodeSpec(nodeInfo.getSpec().getName(), nodeInfo.getSpec().getLabels(), NodeSpec.Role.Manager, nodeInfo.getSpec().getAvailability());
        updateNode(nodeInfo.getID(), nodeInfo.getVersion().getIndex(), nodeSpec);
        log.info("Node {} promoted to a manager in the swarm.", node);
      }
    }
  }

  @Override
  public void demoteNodes(String... nodes) {
    log.info("docker node demote");
    for (String node : nodes) {
      Node nodeInfo = inspectNode(node).getContent();
      if (NodeSpec.Role.Worker.equals(nodeInfo.getSpec().getRole())) {
        log.warn("Node {} is already a worker.", node);
      } else {
        NodeSpec nodeSpec = new NodeSpec(nodeInfo.getSpec().getName(), nodeInfo.getSpec().getLabels(), NodeSpec.Role.Worker, nodeInfo.getSpec().getAvailability());
        updateNode(nodeInfo.getID(), nodeInfo.getVersion().getIndex(), nodeSpec);
        log.info("Manager {} demoted in the swarm.", node);
      }
    }
  }

  @Override
  public EngineResponseContent<List<Task>> tasksOnNode(String node, Map<String, Object> query) {
    log.info("docker node ps");
    Map<String, Object> actualQuery = new HashMap<>();
    if (query != null) {
      actualQuery.putAll(query);
    }
    if (!actualQuery.containsKey("filters")) {
      actualQuery.put("filters", new HashMap<String, Object>());
    }
    Map<String, Object> filters = (Map<String, Object>) actualQuery.get("filters");
    filters.put("node", nodeUtil.resolveNodeId(node));
    new QueryUtil().jsonEncodeQueryParameter(actualQuery, "filters");
    return manageTask.tasks((String) actualQuery.get("filters"));
  }

  @Override
  public EngineResponseContent<List<Task>> tasksOnNode(String node) {
    return tasksOnNode(node, new HashMap<>());
  }
}
