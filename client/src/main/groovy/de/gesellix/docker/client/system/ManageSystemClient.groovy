package de.gesellix.docker.client.system

import de.gesellix.docker.client.DockerAsyncCallback
import de.gesellix.docker.client.DockerAsyncConsumer
import de.gesellix.docker.client.DockerResponse
import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.client.HttpClient
import de.gesellix.util.QueryUtil
import groovy.util.logging.Slf4j

import static java.util.concurrent.Executors.newSingleThreadExecutor

@Slf4j
class ManageSystemClient implements ManageSystem {

    private HttpClient client
    private DockerResponseHandler responseHandler
    private QueryUtil queryUtil

    ManageSystemClient(HttpClient client, DockerResponseHandler responseHandler) {
        this.client = client
        this.responseHandler = responseHandler
        this.queryUtil = new QueryUtil()
    }

    @Override
    systemDf(query = [:]) {
        log.info "docker system df"
        def actualQuery = query ?: [:]
        def response = client.get([path : "/system/df",
                                   query: actualQuery])
        return response
    }

    @Override
    events(DockerAsyncCallback callback, Map query = [:]) {
        log.info "docker events"

        queryUtil.jsonEncodeFilters(query)
        def response = client.get([path : "/events",
                                   query: query,
                                   async: true])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker events failed"))
        def executor = newSingleThreadExecutor()
        def future = executor.submit(new DockerAsyncConsumer(response as DockerResponse, callback))
        response.taskFuture = future
        return response
    }

    @Override
    ping() {
        log.info "docker ping"
        def response = client.get([path: "/_ping", timeout: 2000])
        return response
    }

    @Override
    version() {
        log.info "docker version"
        def response = client.get([path: "/version"])
        return response
    }

    @Override
    info() {
        log.info "docker info"
        def response = client.get([path: "/info"])
        return response
    }
}
