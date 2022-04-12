package de.gesellix.docker.client.system;

import de.gesellix.docker.client.EngineResponseContent;
import de.gesellix.docker.remote.api.EventMessage;
import de.gesellix.docker.remote.api.SystemDataUsageResponse;
import de.gesellix.docker.remote.api.SystemInfo;
import de.gesellix.docker.remote.api.SystemVersion;
import de.gesellix.docker.remote.api.core.StreamCallback;

import java.time.Duration;

public interface ManageSystem {

  EngineResponseContent<SystemDataUsageResponse> systemDf();

  void events(SystemEventsRequest request, StreamCallback<EventMessage> callback, Duration timeout);

  EngineResponseContent<String> ping();

  EngineResponseContent<SystemVersion> version();

  EngineResponseContent<SystemInfo> info();
}
