package de.gesellix.docker.client.tasks

import de.gesellix.docker.client.EngineResponseContent
import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.Task
import de.gesellix.util.QueryUtil
import groovy.util.logging.Slf4j

@Slf4j
class ManageTaskClient implements ManageTask {

  private EngineApiClient client

  ManageTaskClient(EngineApiClient client) {
    this.client = client
  }

  @Override
  EngineResponseContent<List<Task>> tasks(Map<String, Object> query) {
    Map actualQuery = [:]
    if (query) {
      actualQuery.putAll(query)
    }
    new QueryUtil().jsonEncodeQueryParameter(actualQuery, "filters")
    return tasks(actualQuery.filters as String)
  }

  @Override
  EngineResponseContent<List<Task>> tasks(String filters = null) {
    log.info("docker tasks")
    List<Task> tasks = client.taskApi.taskList(filters)
    return new EngineResponseContent<List<Task>>(tasks)
  }

  @Override
  EngineResponseContent<Task> inspectTask(String name) {
    log.info("docker task inspect")
    Task taskInspect = client.taskApi.taskInspect(name)
    return new EngineResponseContent<Task>(taskInspect)
  }
}
