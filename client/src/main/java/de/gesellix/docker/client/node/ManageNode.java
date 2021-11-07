package de.gesellix.docker.client.node;

import de.gesellix.docker.engine.EngineResponse;
import de.gesellix.docker.remote.api.Node;
import de.gesellix.docker.remote.api.NodeSpec;
import de.gesellix.docker.remote.api.Task;

import java.util.List;
import java.util.Map;

public interface ManageNode {

  EngineResponse<Node> inspectNode(String name);

  /**
   * @see #nodes(String)
   * @deprecated use {@link #nodes(String)}
   */
  @Deprecated
  EngineResponse<List<Node>> nodes(Map<String, Object> query);

  EngineResponse<List<Node>> nodes();

  EngineResponse<List<Node>> nodes(String filters);

  void promoteNodes(String[] node);

  void demoteNodes(String[] node);

  void rmNode(String name);

  void updateNode(String name, long version, NodeSpec nodeSpec);

  EngineResponse<List<Task>> tasksOnNode(String node);

  EngineResponse<List<Task>> tasksOnNode(String node, Map<String, Object> query);
}
