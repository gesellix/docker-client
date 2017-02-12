package de.gesellix.docker.client.secret

import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.client.HttpClient
import de.gesellix.util.QueryUtil
import groovy.util.logging.Slf4j

@Slf4j
class ManageSecretClient implements ManageSecret {

    private HttpClient client
    private DockerResponseHandler responseHandler
    private QueryUtil queryUtil

    ManageSecretClient(HttpClient client, DockerResponseHandler responseHandler) {
        this.client = client
        this.responseHandler = responseHandler
        this.queryUtil = new QueryUtil()
    }

    @Override
    createSecret(String name, byte[] secretData, Map<String, String> labels = [:]) {
        log.info "docker secret create"
        // TODO do we need to base64 encode the secret data?
        def secretConfig = [Name  : name,
                            Data  : secretData,
                            Labels: labels]
        def response = client.post([path              : "/secrets/create",
                                    body              : secretConfig,
                                    requestContentType: "application/json"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker secret create failed"))
        return response
    }

    @Override
    inspectSecret(String secretId) {
        log.info "docker secret inspect"
        def response = client.get([path: "/secrets/${secretId}"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker secret inspect failed"))
        return response
    }

    @Override
    secrets() {
        log.info "docker secret ls"
        def response = client.get([path: "/secrets"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker secret ls failed"))
        return response
    }

    @Override
    rmSecret(String secretId) {
        log.info "docker secret rm"
        def response = client.delete([path: "/secrets/${secretId}"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker secret rm failed"))
        return response
    }

    @Override
    updateSecret(String secretId, version, secretSpec) {
        log.info "docker secret update"
        def response = client.post([path              : "/secrets/${secretId}/update",
                                    query             : [version: version],
                                    body              : secretSpec,
                                    requestContentType: "application/json"])
        responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker secret update failed"))
        return response
    }
}
