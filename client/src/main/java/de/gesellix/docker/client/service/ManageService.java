package de.gesellix.docker.client.service;

import de.gesellix.docker.engine.EngineResponse;

import java.util.Map;

public interface ManageService {

  EngineResponse createService(Map<String, Object> config);

  EngineResponse createService(Map<String, Object> config, Map<String, Object> updateOptions);

  EngineResponse inspectService(Object name);

  EngineResponse services();

  EngineResponse services(Map<String, Object> query);

  EngineResponse tasksOfService(String service);

  EngineResponse tasksOfService(String service, Map<String, Object> query);

  EngineResponse rmService(String name);

  EngineResponse scaleService(String name, int replicas);

  EngineResponse updateService(String name, Map<String, Object> query, Map<String, Object> config);

  EngineResponse updateService(String name, Map<String, Object> query, Map<String, Object> config, Map<String, Object> updateOptions);
}
