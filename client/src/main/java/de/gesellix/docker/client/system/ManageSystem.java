package de.gesellix.docker.client.system;

import de.gesellix.docker.engine.EngineResponse;
import de.gesellix.docker.remote.api.EventMessage;
import de.gesellix.docker.remote.api.SystemDataUsageResponse;
import de.gesellix.docker.remote.api.SystemInfo;
import de.gesellix.docker.remote.api.SystemVersion;
import de.gesellix.docker.remote.api.core.StreamCallback;

import java.time.Duration;

public interface ManageSystem {

  EngineResponse<SystemDataUsageResponse> systemDf();

  void events(SystemEventsRequest request, StreamCallback<EventMessage> callback, Duration timeout);

  EngineResponse<String> ping();

  EngineResponse<SystemVersion> version();

  EngineResponse<SystemInfo> info();
}
