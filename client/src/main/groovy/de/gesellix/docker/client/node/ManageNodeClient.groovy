package de.gesellix.docker.client.node

import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.client.HttpClient
import de.gesellix.docker.client.tasks.ManageTask
import de.gesellix.util.QueryUtil
import groovy.util.logging.Slf4j

@Slf4j
class ManageNodeClient implements ManageNode {

    private HttpClient client
    private DockerResponseHandler responseHandler
    private QueryUtil queryUtil
    private ManageTask manageTask
    private NodeUtil nodeUtil

    ManageNodeClient(
            HttpClient client,
            DockerResponseHandler responseHandler,
            ManageTask manageTask,
            NodeUtil nodeUtil) {
        this.client = client
        this.responseHandler = responseHandler
        this.queryUtil = new QueryUtil()
        this.manageTask = manageTask
        this.nodeUtil = nodeUtil
    }

    @Override
    nodes(query = [:]) {
        log.info "docker node ls"
        def actualQuery = query ?: [:]
        queryUtil.jsonEncodeFilters(actualQuery)
        def response = client.get([path : "/nodes",
                                   query: actualQuery])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker node ls failed"))
        return response
    }

    @Override
    inspectNode(name) {
        log.info "docker node inspect"
        def response = client.get([path: "/nodes/$name"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker node inspect failed"))
        return response
    }

    @Override
    rmNode(name) {
        log.info "docker node rm"
        def response = client.delete([path: "/nodes/$name"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker node rm failed"))
        return response
    }

    @Override
    updateNode(name, query, config) {
        log.info "docker node update"
        def actualQuery = query ?: [:]
        config = config ?: [:]
        def response = client.post([path              : "/nodes/$name/update",
                                    query             : actualQuery,
                                    body              : config,
                                    requestContentType: "application/json"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker node update failed"))
        return response
    }

    @Override
    promoteNodes(... nodes) {
        log.info "docker node promote"
        nodes?.each { node ->
            def nodeInfo = inspectNode(node).content
            def nodeSpec = nodeInfo.Spec

            if (nodeSpec.Role == "manager") {
                log.warn("Node ${node} is already a manager.")
            } else {
                nodeSpec.Role = "manager"
                def response = updateNode(
                        node,
                        ["version": nodeInfo.Version.Index],
                        nodeSpec)
                responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker node promote failed for node ${node}"))
                log.info("Node ${node} promoted to a manager in the swarm.")
            }
        }
    }

    @Override
    demoteNodes(... nodes) {
        log.info "docker node demote"
        nodes?.each { node ->
            def nodeInfo = inspectNode(node).content
            def nodeSpec = nodeInfo.Spec

            if (nodeSpec.Role == "worker") {
                log.warn("Node ${node} is already a worker.")
            } else {
                nodeSpec.Role = "worker"
                def response = updateNode(
                        node,
                        ["version": nodeInfo.Version.Index],
                        nodeSpec)
                responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker node demote failed for node ${node}"))
                log.info("Manager ${node} demoted in the swarm.")
            }
        }
    }

    @Override
    tasksOnNode(node, query = [:]) {
        log.info "docker node ps"
        def actualQuery = query ?: [:]
        if (!actualQuery.containsKey('filters')) {
            actualQuery.filters = [:]
        }
        actualQuery.filters['node'] = nodeUtil.resolveNodeId(node)
        return manageTask.tasks(actualQuery)
    }
}
