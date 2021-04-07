package de.gesellix.docker.client.tasks

import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.engine.EngineClient
import groovy.json.JsonBuilder
import spock.lang.Specification

class ManageTaskClientTest extends Specification {

  EngineClient httpClient = Mock(EngineClient)
  ManageTaskClient service

  def setup() {
    service = new ManageTaskClient(httpClient, Mock(DockerResponseHandler))
  }

  def "list tasks with query"() {
    given:
    def filters = [name: ["service-name"]]
    def expectedFilterValue = new JsonBuilder(filters).toString()
    def query = [filters: filters]

    when:
    service.tasks(query)

    then:
    1 * httpClient.get([path : "/tasks",
                        query: [filters: expectedFilterValue]]) >> [status: [success: true]]
  }

  def "inspect task"() {
    when:
    service.inspectTask("task-id")

    then:
    1 * httpClient.get([path: "/tasks/task-id"]) >> [status: [success: true]]
  }
}
