package de.gesellix.docker.client.stack

import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.client.authentication.ManageAuthentication
import de.gesellix.docker.client.config.ManageConfig
import de.gesellix.docker.client.network.ManageNetwork
import de.gesellix.docker.client.node.ManageNode
import de.gesellix.docker.client.secret.ManageSecret
import de.gesellix.docker.client.service.ManageService
import de.gesellix.docker.client.stack.types.StackNetwork
import de.gesellix.docker.client.stack.types.StackSecret
import de.gesellix.docker.client.stack.types.StackService
import de.gesellix.docker.client.system.ManageSystem
import de.gesellix.docker.client.tasks.ManageTask
import de.gesellix.docker.engine.EngineClient
import de.gesellix.docker.engine.EngineResponse
import spock.lang.Specification

import static de.gesellix.docker.client.stack.ManageStackClient.LabelNamespace

class ManageStackClientTest extends Specification {

    EngineClient httpClient = Mock(EngineClient)
    DockerResponseHandler responseHandler = Mock(DockerResponseHandler)
    ManageService manageService = Mock(ManageService)
    ManageTask manageTask = Mock(ManageTask)
    ManageNode manageNode = Mock(ManageNode)
    ManageNetwork manageNetwork = Mock(ManageNetwork)
    ManageSecret manageSecret = Mock(ManageSecret)
    ManageConfig manageConfig = Mock(ManageConfig)
    ManageSystem manageSystem = Mock(ManageSystem)
    ManageAuthentication manageAuthentication = Mock(ManageAuthentication)

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
                manageConfig,
                manageSystem,
                manageAuthentication)
    }

    def "list stacks"() {
        when:
        def stacks = service.lsStacks()

        then:
        1 * manageService.services([filters: [label: [(LabelNamespace): true]]]) >> new EngineResponse(
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
        def expectedResponse = new EngineResponse()

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
        def expectedResponse = new EngineResponse()

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

    def "list services in stack"() {
        given:
        String namespace = "the-stack"
        String namespaceFilter = "${LabelNamespace}=${namespace}"
        def expectedResponse = new EngineResponse()

        when:
        def services = service.stackServices(namespace)

        then:
        1 * manageService.services([filters: [label: [(namespaceFilter): true]]]) >> expectedResponse
        and:
        services == expectedResponse
    }

    def "list filtered services in stack"() {
        given:
        String namespace = "the-stack"
        String namespaceFilter = "${LabelNamespace}=${namespace}"
        def expectedResponse = new EngineResponse()

        when:
        def services = service.stackServices(namespace, [label: [bar: true]])

        then:
        1 * manageService.services([filters: [
                label: [
                        bar              : true,
                        (namespaceFilter): true]]]) >> expectedResponse
        and:
        services == expectedResponse
    }

    def "remove a stack"() {
        given:
        String namespace = "the-stack"
        String namespaceFilter = "${LabelNamespace}=${namespace}"

        when:
        service.stackRm(namespace)

        then:
        1 * manageService.services([filters: [label: [(namespaceFilter): true]]]) >> new EngineResponse(
                content: [[ID: "service1-id"]]
        )
        then:
        1 * manageNetwork.networks([filters: [label: [(namespaceFilter): true]]]) >> new EngineResponse(
                content: [[Id: "network1-id"]]
        )
        then:
        1 * manageSecret.secrets([filters: [label: [(namespaceFilter): true]]]) >> new EngineResponse(
                content: [[ID: "secret1-id"]]
        )

        then:
        1 * manageService.rmService("service1-id")
        then:
        1 * manageNetwork.rmNetwork("network1-id")
        then:
        1 * manageSecret.rmSecret("secret1-id")
    }

    def "deploy an empty stack"() {
        given:
        String namespace = "the-stack"
        String namespaceFilter = "${LabelNamespace}=${namespace}"

        when:
        service.stackDeploy(namespace, new DeployStackConfig(), new DeployStackOptions())

        then:
        manageSystem.info() >> new EngineResponse(content: [Swarm: [ControlAvailable: true]])
        1 * manageNetwork.networks([
                filters: [label: [(namespaceFilter): true]]]) >> new EngineResponse()
        1 * manageService.services([
                filters: ['label': [(namespaceFilter): true]]]) >> new EngineResponse()
    }

    def "deploy a stack"() {
        given:
        String namespace = "the-stack"
        String namespaceFilter = "${LabelNamespace}=${namespace}"
        DeployStackConfig config = new DeployStackConfig()
        config.services["service1"] = new StackService()
        config.networks["network1"] = new StackNetwork(labels: [foo: 'bar'])
        config.secrets["secret1"] = new StackSecret(name: "secret-name-1", data: 'secret'.bytes)

        when:
        service.stackDeploy(namespace, config, new DeployStackOptions())

        then:
        manageSystem.info() >> new EngineResponse(content: [Swarm: [ControlAvailable: true]])

        and:
        1 * manageNetwork.networks([
                filters: [label: [(namespaceFilter): true]]]) >> new EngineResponse()
        1 * manageNetwork.createNetwork("the-stack_network1", [
                'ipam'      : [:],
                'driverOpts': [:],
                'labels'    : [
                        'foo'           : 'bar',
                        (LabelNamespace): namespace],
                'driver'    : null,
                'internal'  : false,
                'attachable': false])

        and:
        1 * manageSecret.secrets([filters: [name: ["secret-name-1"]]]) >> new EngineResponse(
                content: []
        )
        1 * manageSecret.createSecret("secret-name-1", 'secret'.bytes, [(LabelNamespace): namespace])

        and:
        1 * manageService.services([
                filters: ['label': [(namespaceFilter): true]]]) >> new EngineResponse()
        1 * manageService.createService([
                'endpointSpec': [:],
                'taskTemplate': [:],
                'mode'        : [:],
                'labels'      : [(LabelNamespace): namespace],
                'updateConfig': [:],
                'networks'    : [:],
                'name'        : 'the-stack_service1'],
                [:])
    }
}
