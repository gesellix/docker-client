package de.gesellix.docker.client.secret

import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.engine.EngineClient
import de.gesellix.docker.engine.EngineResponse
import de.gesellix.util.QueryUtil
import groovy.util.logging.Slf4j

@Slf4j
class ManageSecretClient implements ManageSecret {

  private EngineClient client
  private DockerResponseHandler responseHandler
  private QueryUtil queryUtil

  ManageSecretClient(EngineClient client, DockerResponseHandler responseHandler) {
    this.client = client
    this.responseHandler = responseHandler
    this.queryUtil = new QueryUtil()
  }

  @Override
  EngineResponse createSecret(String name, byte[] secretData, Map<String, String> labels = [:]) {
    log.info "docker secret create"
    def secretDataBase64 = Base64.encoder.encode(secretData)
    def secretConfig = [Name  : name,
                        Data  : secretDataBase64,
                        Labels: labels]
    def response = client.post([path              : "/secrets/create",
                                body              : secretConfig,
                                requestContentType: "application/json"])
    responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker secret create failed"))
    return response
  }

  @Override
  EngineResponse inspectSecret(String secretId) {
    log.info "docker secret inspect"
    def response = client.get([path: "/secrets/${secretId}".toString()])
    responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker secret inspect failed"))
    return response
  }

  @Override
  EngineResponse secrets(Map query = [:]) {
    log.info "docker secret ls"
    def actualQuery = query ?: [:]
    queryUtil.jsonEncodeFilters(actualQuery)
    def response = client.get([path : "/secrets",
                               query: actualQuery])
    responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker secret ls failed"))
    return response
  }

  @Override
  EngineResponse rmSecret(String secretId) {
    log.info "docker secret rm"
    def response = client.delete([path: "/secrets/${secretId}".toString()])
    responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker secret rm failed"))
    return response
  }

  @Override
  EngineResponse updateSecret(String secretId, version, secretSpec) {
    log.info "docker secret update"
    def response = client.post([path              : "/secrets/${secretId}/update".toString(),
                                query             : [version: version],
                                body              : secretSpec,
                                requestContentType: "application/json"])
    responseHandler.ensureSuccessfulResponse(response, new IllegalStateException("docker secret update failed"))
    return response
  }
}
