package de.gesellix.docker.client.distribution

import de.gesellix.docker.client.DockerResponse
import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.client.HttpClient
import groovy.util.logging.Slf4j

@Slf4j
class ManageDistributionService implements ManageDistribution {

    private HttpClient client
    private DockerResponseHandler responseHandler

    ManageDistributionService(HttpClient client, DockerResponseHandler responseHandler) {
        this.client = client
        this.responseHandler = responseHandler
    }

    @Override
    DockerResponse descriptor(String image) {
        log.info "docker distribution descriptor"
        def response = client.get([path: "/distribution/${image}/json"])
        return response
    }
}
