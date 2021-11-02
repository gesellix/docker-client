package de.gesellix.docker.client.tasks;

import de.gesellix.docker.engine.EngineResponse;

import java.util.Map;

public interface ManageTask {

  EngineResponse tasks();

  EngineResponse tasks(Map<String, Object> query);

  EngineResponse inspectTask(String name);
}
