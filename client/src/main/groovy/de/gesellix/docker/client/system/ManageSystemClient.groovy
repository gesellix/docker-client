package de.gesellix.docker.client.system

import de.gesellix.docker.client.EngineResponseContent
import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.EventMessage
import de.gesellix.docker.remote.api.SystemDataUsageResponse
import de.gesellix.docker.remote.api.SystemInfo
import de.gesellix.docker.remote.api.SystemVersion
import de.gesellix.docker.remote.api.core.StreamCallback
import groovy.util.logging.Slf4j

import java.time.Duration

@Slf4j
class ManageSystemClient implements ManageSystem {

  private EngineApiClient client

  ManageSystemClient(EngineApiClient client) {
    this.client = client
  }

  @Override
  EngineResponseContent<SystemDataUsageResponse> systemDf() {
    log.info("docker system df")
    SystemDataUsageResponse systemDataUsage = client.getSystemApi().systemDataUsage()
    return new EngineResponseContent(systemDataUsage)
  }

  @Override
  void events(SystemEventsRequest request, StreamCallback<EventMessage> callback, Duration timeout) {
    log.info("docker events")
    client.getSystemApi().systemEvents(request.since, request.until, request.filters, callback, timeout.toMillis())
  }

  @Override
  EngineResponseContent<String> ping() {
    log.info("docker ping")
    String systemPing = client.getSystemApi().systemPing()
    return new EngineResponseContent(systemPing)
  }

  @Override
  EngineResponseContent<SystemVersion> version() {
    log.info("docker version")
    SystemVersion systemVersion = client.getSystemApi().systemVersion()
    return new EngineResponseContent(systemVersion)
  }

  @Override
  EngineResponseContent<SystemInfo> info() {
    log.info("docker info")
    SystemInfo systemInfo = client.getSystemApi().systemInfo()
    return new EngineResponseContent(systemInfo)
  }
}
