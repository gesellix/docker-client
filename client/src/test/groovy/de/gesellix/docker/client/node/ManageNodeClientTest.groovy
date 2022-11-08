package de.gesellix.docker.client.node

import de.gesellix.docker.client.tasks.ManageTask
import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.Node
import de.gesellix.docker.remote.api.NodeSpec
import de.gesellix.docker.remote.api.ObjectVersion
import de.gesellix.docker.remote.api.client.NodeApi
import spock.lang.Specification

class ManageNodeClientTest extends Specification {

  EngineApiClient client = Mock(EngineApiClient)
  ManageTask manageTask = Mock(ManageTask)
  NodeUtil nodeUtil = Mock(NodeUtil)

  ManageNodeClient service

  def setup() {
    service = new ManageNodeClient(client, manageTask, nodeUtil)
  }

  def "list nodes"() {
    given:
    def nodeApi = Mock(NodeApi)
    client.nodeApi >> nodeApi
    def nodes = Mock(List)
    def filters = '{"membership":["accepted"],"role":["worker"]}'

    when:
    def responseContent = service.nodes(filters)

    then:
    1 * client.nodeApi.nodeList(filters) >> nodes
    responseContent.content == nodes
  }

  def "inspect node"() {
    given:
    def nodeApi = Mock(NodeApi)
    client.nodeApi >> nodeApi
    def node = Mock(Node)

    when:
    def inspectNode = service.inspectNode("node-id")

    then:
    1 * nodeApi.nodeInspect("node-id") >> node
    inspectNode.content == node
  }

  def "update node"() {
    given:
    def nodeApi = Mock(NodeApi)
    client.nodeApi >> nodeApi
    def nodeSpec = new NodeSpec(
        null,
        null,
        null,
        NodeSpec.Availability.Active
    )

    when:
    service.updateNode("node-id", 42, nodeSpec)

    then:
    1 * nodeApi.nodeUpdate("node-id", 42, nodeSpec)
  }

  def "promote nodes"() {
    given:
    def nodeApi = Mock(NodeApi)
    client.nodeApi >> nodeApi
    def node1Info = new Node("ID1", new ObjectVersion(23), null, null,
                             new NodeSpec("node-1", null, NodeSpec.Role.Worker, null), null, null, null)
    def node1UpdatedSpec = new NodeSpec("node-1", null, NodeSpec.Role.Manager, null)
    def node2Info = new Node("ID2", new ObjectVersion(43), null, null,
                             new NodeSpec(null, null, NodeSpec.Role.Manager, null), null, null, null)

    when:
    service.promoteNodes("node-1", "node-2")

    then:
    1 * nodeApi.nodeInspect("node-1") >> node1Info
    1 * nodeApi.nodeInspect("node-2") >> node2Info
    1 * nodeApi.nodeUpdate("ID1", 23, node1UpdatedSpec)
  }

  def "demote nodes"() {
    given:
    def nodeApi = Mock(NodeApi)
    client.nodeApi >> nodeApi
    def node1Info = new Node("ID1", new ObjectVersion(23), null, null,
                             new NodeSpec("node-1", null, NodeSpec.Role.Worker, null), null, null, null)
    def node2Info = new Node("ID2", new ObjectVersion(43), null, null,
                             new NodeSpec("node-2", null, NodeSpec.Role.Manager, null), null, null, null)
    def node2UpdatedSpec = new NodeSpec("node-2", null, NodeSpec.Role.Worker, null)

    when:
    service.demoteNodes("node-1", "node-2")

    then:
    1 * nodeApi.nodeInspect("node-1") >> node1Info
    1 * nodeApi.nodeInspect("node-2") >> node2Info
    1 * nodeApi.nodeUpdate("ID2", 43, node2UpdatedSpec)
  }

  def "rm node"() {
    given:
    def nodeApi = Mock(NodeApi)
    client.nodeApi >> nodeApi

    when:
    service.rmNode("node-id")

    then:
    1 * nodeApi.nodeDelete("node-id", null)
  }

  def "list tasks on node 'self' with query"() {
    given:
    def originalQuery = [filters: [param: "value"]]
    def modifiedQuery = '{"param":"value","node":"node-id"}'

    when:
    service.tasksOnNode("self", originalQuery)

    then:
    1 * nodeUtil.resolveNodeId('self') >> "node-id"
    1 * manageTask.tasks(modifiedQuery)
  }
}
