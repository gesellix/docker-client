package de.gesellix.docker.client.volume

import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.client.HttpClient
import de.gesellix.util.QueryUtil
import groovy.util.logging.Slf4j

@Slf4j
class ManageVolumeClient implements ManageVolume {

    private HttpClient client
    private DockerResponseHandler responseHandler
    private QueryUtil queryUtil

    ManageVolumeClient(HttpClient client, DockerResponseHandler responseHandler) {
        this.client = client
        this.responseHandler = responseHandler
        this.queryUtil = new QueryUtil()
    }

    @Override
    volumes(query = [:]) {
        log.info "docker volume ls"
        def actualQuery = query ?: [:]
        queryUtil.jsonEncodeFilters(actualQuery)
        def response = client.get([path : "/volumes",
                                   query: actualQuery])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker volume ls failed"))
        return response
    }

    @Override
    inspectVolume(name) {
        log.info "docker volume inspect"
        def response = client.get([path: "/volumes/$name"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker volume inspect failed"))
        return response
    }

    @Override
    createVolume(config = [:]) {
        log.info "docker volume create"
        def response = client.post([path              : "/volumes/create",
                                    body              : config ?: [:],
                                    requestContentType: "application/json"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker volume create failed"))
        return response
    }

    @Override
    rmVolume(name) {
        log.info "docker volume rm"
        def response = client.delete([path: "/volumes/$name"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker volume rm failed"))
        return response
    }

    @Override
    pruneVolumes(query = [:]) {
        log.info "docker volume prune"
        def actualQuery = query ?: [:]
        queryUtil.jsonEncodeFilters(actualQuery)
        def response = client.post([path : "/volumes/prune",
                                    query: actualQuery])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker volume prune failed"))
        return response
    }
}
