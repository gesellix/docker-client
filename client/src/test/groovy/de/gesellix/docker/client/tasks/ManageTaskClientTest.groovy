package de.gesellix.docker.client.tasks

import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.Task
import de.gesellix.docker.remote.api.client.TaskApi
import spock.lang.Specification

class ManageTaskClientTest extends Specification {

  EngineApiClient client = Mock(EngineApiClient)
  ManageTaskClient service

  def setup() {
    service = new ManageTaskClient(client)
  }

  def "list tasks"() {
    given:
    def taskApi = Mock(TaskApi)
    client.taskApi >> taskApi
    def tasks = Mock(List)
    def filters = '{"name":["service-name"]}'

    when:
    def responseContent = service.tasks(filters)

    then:
    1 * taskApi.taskList(filters) >> tasks
    responseContent.content == tasks
  }

  def "inspect task"() {
    given:
    def taskApi = Mock(TaskApi)
    client.taskApi >> taskApi
    def task = Mock(Task)

    when:
    def inspectTask = service.inspectTask("task-id")

    then:
    1 * taskApi.taskInspect("task-id") >> task
    inspectTask.content == task
  }
}
