package de.gesellix.docker.client.stack

import de.gesellix.docker.client.DockerResponse
import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.client.HttpClient
import de.gesellix.docker.client.network.ManageNetwork
import de.gesellix.docker.client.node.ManageNode
import de.gesellix.docker.client.secret.ManageSecret
import de.gesellix.docker.client.service.ManageService
import de.gesellix.docker.client.system.ManageSystem
import de.gesellix.docker.client.tasks.ManageTask
import spock.lang.Specification

import static de.gesellix.docker.client.stack.ManageStackClient.LabelNamespace

class ManageStackClientTest extends Specification {

    HttpClient httpClient = Mock(HttpClient)
    DockerResponseHandler responseHandler = Mock(DockerResponseHandler)
    ManageService manageService = Mock(ManageService)
    ManageTask manageTask = Mock(ManageTask)
    ManageNode manageNode = Mock(ManageNode)
    ManageNetwork manageNetwork = Mock(ManageNetwork)
    ManageSecret manageSecret = Mock(ManageSecret)
    ManageSystem manageSystem = Mock(ManageSystem)

    ManageStackClient service

    def setup() {
        service = new ManageStackClient(
                httpClient,
                responseHandler,
                manageService,
                manageTask,
                manageNode,
                manageNetwork,
                manageSecret,
                manageSystem)
    }

    def "list stacks"() {
        when:
        def stacks = service.lsStacks()

        then:
        1 * manageService.services([filters: [label: [(LabelNamespace): true]]]) >> new DockerResponse(
                content: [
                        [Spec: [Labels: [(LabelNamespace): "service1"]]],
                        [Spec: [Labels: [(LabelNamespace): "service2"]]],
                        [Spec: [Labels: [(LabelNamespace): "service1"]]]
                ]
        )
        and:
        stacks as List == [
                new ManageStackClient.Stack(name: "service1", services: 2),
                new ManageStackClient.Stack(name: "service2", services: 1)
        ]
    }

    def "list tasks in stack"() {
        given:
        String namespace = "the-stack"
        String namespaceFilter = "${LabelNamespace}=${namespace}"
        def expectedResponse = new DockerResponse()

        when:
        def tasks = service.stackPs(namespace)

        then:
        1 * manageTask.tasks([filters: [label: [(namespaceFilter): true]]]) >> expectedResponse
        and:
        tasks == expectedResponse
    }

    def "list filtered tasks in stack"() {
        given:
        String namespace = "the-stack"
        String namespaceFilter = "${LabelNamespace}=${namespace}"
        def expectedResponse = new DockerResponse()

        when:
        def tasks = service.stackPs(namespace, [label: [foo: true]])

        then:
        1 * manageTask.tasks([filters: [
                label: [
                        foo              : true,
                        (namespaceFilter): true]]]) >> expectedResponse
        and:
        tasks == expectedResponse
    }
}
