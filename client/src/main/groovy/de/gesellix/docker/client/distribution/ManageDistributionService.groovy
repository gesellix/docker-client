package de.gesellix.docker.client.distribution

import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.engine.EngineClient
import de.gesellix.docker.engine.EngineResponse
import groovy.util.logging.Slf4j

@Slf4j
class ManageDistributionService implements ManageDistribution {

    private EngineClient client
    private DockerResponseHandler responseHandler

    ManageDistributionService(EngineClient client, DockerResponseHandler responseHandler) {
        this.client = client
        this.responseHandler = responseHandler
    }

    @Override
    EngineResponse descriptor(String image) {
        log.info "docker distribution descriptor"
        def response = client.get([path: "/distribution/${image}/json"])
        return response
    }
}
