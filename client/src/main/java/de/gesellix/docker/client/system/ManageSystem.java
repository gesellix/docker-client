package de.gesellix.docker.client.system;

import de.gesellix.docker.client.DockerAsyncCallback;
import de.gesellix.docker.engine.EngineResponse;

import java.util.Map;

public interface ManageSystem {

  EngineResponse systemDf();

  EngineResponse events(DockerAsyncCallback callback);

  EngineResponse events(DockerAsyncCallback callback, Map query);

  EngineResponse ping();

  EngineResponse version();

  EngineResponse info();
}
