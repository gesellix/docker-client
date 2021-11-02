package de.gesellix.docker.client.node;

import de.gesellix.docker.engine.EngineResponse;

import java.util.List;
import java.util.Map;

public interface ManageNode {

  Object demoteNodes(String[] node);

  Object inspectNode(String name);

  EngineResponse nodes();

  EngineResponse nodes(Map<String, Object> query);

  void promoteNodes(String[] node);

  EngineResponse tasksOnNode(String node);

  EngineResponse tasksOnNode(String node, Map<String, Object> query);

  Object rmNode(Object name);

  EngineResponse updateNode(String name, Map<String, Object> query, Map<String, Object> config);
}
