package de.gesellix.docker.client;

import de.gesellix.docker.client.testutil.DockerEngineAvailable;
import de.gesellix.docker.client.testutil.InjectDockerClient;
import de.gesellix.docker.remote.api.LocalNodeState;
import de.gesellix.docker.remote.api.Node;
import de.gesellix.docker.remote.api.NodeSpec;
import de.gesellix.docker.remote.api.NodeState;
import de.gesellix.docker.remote.api.client.NodeApi;
import de.gesellix.docker.remote.api.core.ClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.gesellix.docker.client.testutil.Constants.LABEL_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DockerEngineAvailable(requiredSwarmMode = LocalNodeState.Active)
class NodeApiIntegrationTest {

  @InjectDockerClient
  private TypeSafeDockerClientImpl typeSafeDockerClient;

  NodeApi nodeApi;

  @BeforeEach
  public void setup() {
    nodeApi = typeSafeDockerClient.getNodeApi();
  }

  @Test
  public void nodeListInspectUpdate() {
    List<Node> nodes = nodeApi.nodeList(null);
    assertFalse(nodes.isEmpty());
    Node firstNode = nodes.get(0);
    assertEquals(NodeState.Ready, firstNode.getStatus().getState());

    Node node = nodeApi.nodeInspect(firstNode.getID());
    assertEquals(firstNode.getID(), node.getID());

    NodeSpec originalSpec = node.getSpec();
    assertFalse(originalSpec.getLabels().containsKey(LABEL_KEY));

    Map<String, String> labels = new HashMap<>(originalSpec.getLabels());
    labels.put(LABEL_KEY, "temporary");
    NodeSpec spec = new NodeSpec(originalSpec.getName(), labels, originalSpec.getRole(), originalSpec.getAvailability());
    nodeApi.nodeUpdate(firstNode.getID(), node.getVersion().getIndex().longValue(), spec);

    node = nodeApi.nodeInspect(firstNode.getID());
    assertEquals(node.getSpec().getLabels().get(LABEL_KEY), "temporary");

    nodeApi.nodeUpdate(firstNode.getID(), node.getVersion().getIndex().longValue(), originalSpec);
  }

  @Test
  public void nodeDelete() {
    List<Node> nodes = nodeApi.nodeList(null);
    Node firstNode = nodes.get(0);
    String error = assertThrows(ClientException.class, () -> nodeApi.nodeDelete(firstNode.getID(), false)).toString();
    assertTrue(error.contains("FailedPrecondition"));
  }
}
