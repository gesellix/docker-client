package de.gesellix.docker.client.node

import de.gesellix.docker.client.EngineResponseContent
import de.gesellix.docker.client.tasks.ManageTask
import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.Node
import de.gesellix.docker.remote.api.NodeSpec
import de.gesellix.docker.remote.api.Task
import de.gesellix.util.QueryUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ManageNodeClient implements ManageNode {

  private final Logger log = LoggerFactory.getLogger(ManageNodeClient)

  private EngineApiClient client
  private ManageTask manageTask
  private NodeUtil nodeUtil

  ManageNodeClient(
      EngineApiClient client,
      ManageTask manageTask,
      NodeUtil nodeUtil) {
    this.client = client
    this.manageTask = manageTask
    this.nodeUtil = nodeUtil
  }

  @Override
  EngineResponseContent<List<Node>> nodes(Map<String, Object> query) {
    Map actualQuery = [:]
    if (query) {
      actualQuery.putAll(query)
    }
    new QueryUtil().jsonEncodeQueryParameter(actualQuery, "filters")
    return nodes(actualQuery.filters as String)
  }

  @Override
  EngineResponseContent<List<Node>> nodes(String filters = null) {
    log.info("docker node ls")
    List<Node> response = client.nodeApi.nodeList(filters)
    return new EngineResponseContent<List<Node>>(response)
  }

  @Override
  EngineResponseContent<Node> inspectNode(String name) {
    log.info("docker node inspect")
    Node nodeInspect = client.nodeApi.nodeInspect(name)
    return new EngineResponseContent<Node>(nodeInspect)
  }

  @Override
  void rmNode(String name) {
    log.info("docker node rm")
    client.nodeApi.nodeDelete(name, null)
  }

  @Override
  void updateNode(String name, long version, NodeSpec nodeSpec) {
    log.info("docker node update")
    client.nodeApi.nodeUpdate(name, version, nodeSpec)
  }

  @Override
  void promoteNodes(String... nodes) {
    log.info("docker node promote")
    nodes?.each { String node ->
      Node nodeInfo = inspectNode(node).content
      if (nodeInfo.spec.role == NodeSpec.Role.Manager) {
        log.warn("Node ${node} is already a manager.")
      }
      else {
        def nodeSpec = new NodeSpec(nodeInfo.spec.name, nodeInfo.spec.labels, NodeSpec.Role.Manager, nodeInfo.spec.availability)
        updateNode(nodeInfo.ID, nodeInfo.version.index, nodeSpec)
        log.info("Node ${node} promoted to a manager in the swarm.")
      }
    }
  }

  @Override
  void demoteNodes(String... nodes) {
    log.info("docker node demote")
    nodes?.each { String node ->
      Node nodeInfo = inspectNode(node).content
      if (nodeInfo.spec.role == NodeSpec.Role.Worker) {
        log.warn("Node ${node} is already a worker.")
      }
      else {
        def nodeSpec = new NodeSpec(nodeInfo.spec.name, nodeInfo.spec.labels, NodeSpec.Role.Worker, nodeInfo.spec.availability)
        updateNode(nodeInfo.ID, nodeInfo.version.index, nodeSpec)
        log.info("Manager ${node} demoted in the swarm.")
      }
    }
  }

  @Override
  EngineResponseContent<List<Task>> tasksOnNode(String node, Map<String, Object> query = [:]) {
    log.info("docker node ps")
    Map actualQuery = query ?: [:]
    if (!actualQuery.containsKey('filters')) {
      actualQuery.filters = [:]
    }
    actualQuery.filters['node'] = nodeUtil.resolveNodeId(node)
    return manageTask.tasks(actualQuery)
  }
}
