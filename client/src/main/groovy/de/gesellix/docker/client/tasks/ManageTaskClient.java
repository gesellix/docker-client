package de.gesellix.docker.client.tasks;

import de.gesellix.docker.client.EngineResponseContent;
import de.gesellix.docker.remote.api.EngineApiClient;
import de.gesellix.docker.remote.api.Task;
import de.gesellix.util.QueryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageTaskClient implements ManageTask {

  private final Logger log = LoggerFactory.getLogger(ManageTaskClient.class);
  private final EngineApiClient client;

  public ManageTaskClient(EngineApiClient client) {
    this.client = client;
  }

  /**
   * @see #tasks(String)
   * @deprecated use {@link #tasks(String)}
   */
  @Deprecated
  @Override
  public EngineResponseContent<List<Task>> tasks(Map<String, Object> query) {
    Map<String, Object> actualQuery = new HashMap<>();
    if (query != null) {
      actualQuery.putAll(query);
    }
    new QueryUtil().jsonEncodeQueryParameter(actualQuery, "filters");
    return tasks((String) actualQuery.get("filters"));
  }

  @Override
  public EngineResponseContent<List<Task>> tasks(String filters) {
    log.info("docker tasks");
    List<Task> tasks = client.getTaskApi().taskList(filters);
    return new EngineResponseContent<>(tasks);
  }

  @Override
  public EngineResponseContent<List<Task>> tasks() {
    return tasks((String) null);
  }

  @Override
  public EngineResponseContent<Task> inspectTask(String name) {
    log.info("docker task inspect");
    Task taskInspect = client.getTaskApi().taskInspect(name);
    return new EngineResponseContent<>(taskInspect);
  }
}
