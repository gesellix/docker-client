package de.gesellix.docker.client.config

import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.engine.EngineClient
import de.gesellix.docker.engine.EngineResponse
import de.gesellix.util.QueryUtil
import groovy.util.logging.Slf4j

@Slf4j
class ManageConfigClient implements ManageConfig {

    private EngineClient client
    private DockerResponseHandler responseHandler
    private QueryUtil queryUtil

    ManageConfigClient(EngineClient client, DockerResponseHandler responseHandler) {
        this.client = client
        this.responseHandler = responseHandler
        this.queryUtil = new QueryUtil()
    }

    @Override
    EngineResponse createConfig(String name, byte[] configData, Map<String, String> labels = [:]) {
        log.info "docker config create"
        // TODO do we need to base64 encode the config data?
        def configConfig = [Name  : name,
                            Data  : configData,
                            Labels: labels]
        def response = client.post([path              : "/configs/create",
                                    body              : configConfig,
                                    requestContentType: "application/json"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker config create failed"))
        return response
    }

    @Override
    EngineResponse inspectConfig(String configId) {
        log.info "docker config inspect"
        def response = client.get([path: "/configs/${configId}"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker config inspect failed"))
        return response
    }

    @Override
    EngineResponse configs(Map query = [:]) {
        log.info "docker config ls"
        def actualQuery = query ?: [:]
        queryUtil.jsonEncodeFilters(actualQuery)
        def response = client.get([path : "/configs",
                                   query: actualQuery])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker config ls failed"))
        return response
    }

    @Override
    EngineResponse rmConfig(String configId) {
        log.info "docker config rm"
        def response = client.delete([path: "/configs/${configId}"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker config rm failed"))
        return response
    }

    @Override
    EngineResponse updateConfig(String configId, version, configSpec) {
        log.info "docker config update"
        def response = client.post([path              : "/configs/${configId}/update",
                                    query             : [version: version],
                                    body              : configSpec,
                                    requestContentType: "application/json"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker config update failed"))
        return response
    }
}
