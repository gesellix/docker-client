package de.gesellix.docker.client.system;

import de.gesellix.docker.client.EngineResponseContent;
import de.gesellix.docker.remote.api.EngineApiClient;
import de.gesellix.docker.remote.api.EventMessage;
import de.gesellix.docker.remote.api.SystemDataUsageResponse;
import de.gesellix.docker.remote.api.SystemInfo;
import de.gesellix.docker.remote.api.SystemVersion;
import de.gesellix.docker.remote.api.core.StreamCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class ManageSystemClient implements ManageSystem {

  private final Logger log = LoggerFactory.getLogger(ManageSystemClient.class);
  private final EngineApiClient client;

  public ManageSystemClient(EngineApiClient client) {
    this.client = client;
  }

  @Override
  public EngineResponseContent<SystemDataUsageResponse> systemDf() {
    log.info("docker system df");
    SystemDataUsageResponse systemDataUsage = client.getSystemApi().systemDataUsage();
    return new EngineResponseContent<>(systemDataUsage);
  }

  @Override
  public void events(SystemEventsRequest request, StreamCallback<EventMessage> callback, Duration timeout) {
    log.info("docker events");
    client.getSystemApi().systemEvents(request.getSince(), request.getUntil(), request.getFilters(), callback, timeout.toMillis());
  }

  @Override
  public EngineResponseContent<String> ping() {
    log.info("docker ping");
    String systemPing = client.getSystemApi().systemPing();
    return new EngineResponseContent<>(systemPing);
  }

  @Override
  public EngineResponseContent<SystemVersion> version() {
    log.info("docker version");
    SystemVersion systemVersion = client.getSystemApi().systemVersion();
    return new EngineResponseContent<>(systemVersion);
  }

  @Override
  public EngineResponseContent<SystemInfo> info() {
    log.info("docker info");
    SystemInfo systemInfo = client.getSystemApi().systemInfo();
    return new EngineResponseContent<>(systemInfo);
  }
}
