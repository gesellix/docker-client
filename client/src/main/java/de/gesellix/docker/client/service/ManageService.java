package de.gesellix.docker.client.service;

import de.gesellix.docker.engine.EngineResponse;
import de.gesellix.docker.remote.api.Service;
import de.gesellix.docker.remote.api.ServiceCreateResponse;
import de.gesellix.docker.remote.api.ServiceSpec;
import de.gesellix.docker.remote.api.ServiceUpdateResponse;
import de.gesellix.docker.remote.api.Task;

import java.util.List;
import java.util.Map;

public interface ManageService {

  EngineResponse<ServiceCreateResponse> createService(ServiceSpec serviceSpec);

  EngineResponse<ServiceCreateResponse> createService(ServiceSpec serviceSpec, String encodedRegistryAuth);

  EngineResponse<Service> inspectService(String name);

  EngineResponse<List<Service>> services(Map<String, Object> query);

  EngineResponse<List<Service>> services();

  EngineResponse<List<Service>> services(String filters);

  EngineResponse<List<Service>> services(String filters, Boolean status);

  EngineResponse<List<Task>> tasksOfService(String service);

  EngineResponse<List<Task>> tasksOfService(String service, Map<String, Object> query);

  void rmService(String name);

  EngineResponse<ServiceUpdateResponse> scaleService(String name, int replicas);

  EngineResponse<ServiceUpdateResponse> updateService(String name, int version, String rollback);

  EngineResponse<ServiceUpdateResponse> updateService(String name, int version, String rollback, String registryAuthFrom);

  EngineResponse<ServiceUpdateResponse> updateService(String name, int version, String rollback, String registryAuthFrom, String encodedRegistryAuth);

  EngineResponse<ServiceUpdateResponse> updateService(String name, int version, ServiceSpec serviceSpec);

  EngineResponse<ServiceUpdateResponse> updateService(String name, int version, ServiceSpec serviceSpec, String registryAuthFrom);

  EngineResponse<ServiceUpdateResponse> updateService(String name, int version, ServiceSpec serviceSpec, String registryAuthFrom, String encodedRegistryAuth);
}
