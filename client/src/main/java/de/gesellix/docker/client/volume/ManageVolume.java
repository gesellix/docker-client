package de.gesellix.docker.client.volume;

import de.gesellix.docker.engine.EngineResponse;

import java.util.Map;

public interface ManageVolume {

  EngineResponse createVolume();

  EngineResponse createVolume(Map<String, Object> config);

  EngineResponse inspectVolume(String name);

  EngineResponse volumes();

  EngineResponse volumes(Map<String, Object> query);

  EngineResponse pruneVolumes();

  EngineResponse pruneVolumes(Map<String, Object> query);

  EngineResponse rmVolume(String name);
}
