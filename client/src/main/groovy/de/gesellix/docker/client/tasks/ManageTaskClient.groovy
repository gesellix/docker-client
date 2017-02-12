package de.gesellix.docker.client.tasks

import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.client.HttpClient
import de.gesellix.util.QueryUtil
import groovy.util.logging.Slf4j

@Slf4j
class ManageTaskClient implements ManageTask {

    private HttpClient client
    private DockerResponseHandler responseHandler
    private QueryUtil queryUtil

    ManageTaskClient(HttpClient client, DockerResponseHandler responseHandler) {
        this.client = client
        this.responseHandler = responseHandler
        this.queryUtil = new QueryUtil()
    }

    @Override
    tasks(query = [:]) {
        log.info "docker tasks"
        def actualQuery = query ?: [:]
        queryUtil.jsonEncodeFilters(actualQuery)
        def response = client.get([path : "/tasks",
                                   query: actualQuery])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker tasks failed"))
        return response
    }

    @Override
    inspectTask(name) {
        log.info "docker task inspect"
        def response = client.get([path: "/tasks/$name"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker task inspect failed"))
        return response
    }
}
