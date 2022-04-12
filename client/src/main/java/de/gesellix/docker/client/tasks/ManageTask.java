package de.gesellix.docker.client.tasks;

import de.gesellix.docker.client.EngineResponseContent;
import de.gesellix.docker.remote.api.Task;

import java.util.List;
import java.util.Map;

public interface ManageTask {

  /**
   * @see #tasks(String)
   * @deprecated use {@link #tasks(String)}
   */
  @Deprecated
  EngineResponseContent<List<Task>> tasks(Map<String, Object> query);

  EngineResponseContent<List<Task>> tasks();

  EngineResponseContent<List<Task>> tasks(String filters);

  EngineResponseContent<Task> inspectTask(String name);
}
