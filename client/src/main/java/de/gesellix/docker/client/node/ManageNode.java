package de.gesellix.docker.client.node;

import de.gesellix.docker.client.EngineResponseContent;
import de.gesellix.docker.remote.api.Node;
import de.gesellix.docker.remote.api.NodeSpec;
import de.gesellix.docker.remote.api.Task;

import java.util.List;
import java.util.Map;

public interface ManageNode {

  EngineResponseContent<Node> inspectNode(String name);

  /**
   * @see #nodes(String)
   * @deprecated use {@link #nodes(String)}
   */
  @Deprecated
  EngineResponseContent<List<Node>> nodes(Map<String, Object> query);

  EngineResponseContent<List<Node>> nodes();

  EngineResponseContent<List<Node>> nodes(String filters);

  void promoteNodes(String[] node);

  void demoteNodes(String[] node);

  void rmNode(String name);

  void updateNode(String name, long version, NodeSpec nodeSpec);

  EngineResponseContent<List<Task>> tasksOnNode(String node);

  EngineResponseContent<List<Task>> tasksOnNode(String node, Map<String, Object> query);
}
