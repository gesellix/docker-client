package de.gesellix.docker.client.tasks;

import de.gesellix.docker.engine.EngineResponse;
import de.gesellix.docker.remote.api.Task;

import java.util.List;
import java.util.Map;

public interface ManageTask {

  /**
   * @see #tasks(String)
   * @deprecated use {@link #tasks(String)}
   */
  @Deprecated
  EngineResponse<List<Task>> tasks(Map<String, Object> query);

  EngineResponse<List<Task>> tasks();

  EngineResponse<List<Task>> tasks(String filters);

  EngineResponse<Task> inspectTask(String name);
}
