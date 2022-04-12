package de.gesellix.docker.client.service;

import de.gesellix.docker.client.EngineResponseContent;
import de.gesellix.docker.remote.api.Service;
import de.gesellix.docker.remote.api.ServiceCreateResponse;
import de.gesellix.docker.remote.api.ServiceSpec;
import de.gesellix.docker.remote.api.ServiceUpdateResponse;
import de.gesellix.docker.remote.api.Task;

import java.util.List;
import java.util.Map;

public interface ManageService {

  EngineResponseContent<ServiceCreateResponse> createService(ServiceSpec serviceSpec);

  EngineResponseContent<ServiceCreateResponse> createService(ServiceSpec serviceSpec, String encodedRegistryAuth);

  EngineResponseContent<Service> inspectService(String name);

  EngineResponseContent<List<Service>> services(Map<String, Object> query);

  EngineResponseContent<List<Service>> services();

  EngineResponseContent<List<Service>> services(String filters);

  EngineResponseContent<List<Service>> services(String filters, Boolean status);

  EngineResponseContent<List<Task>> tasksOfService(String service);

  EngineResponseContent<List<Task>> tasksOfService(String service, Map<String, Object> query);

  void rmService(String name);

  EngineResponseContent<ServiceUpdateResponse> scaleService(String name, int replicas);

  EngineResponseContent<ServiceUpdateResponse> updateService(String name, int version, String rollback);

  EngineResponseContent<ServiceUpdateResponse> updateService(String name, int version, String rollback, String registryAuthFrom);

  EngineResponseContent<ServiceUpdateResponse> updateService(String name, int version, String rollback, String registryAuthFrom, String encodedRegistryAuth);

  EngineResponseContent<ServiceUpdateResponse> updateService(String name, int version, ServiceSpec serviceSpec);

  EngineResponseContent<ServiceUpdateResponse> updateService(String name, int version, ServiceSpec serviceSpec, String registryAuthFrom);

  EngineResponseContent<ServiceUpdateResponse> updateService(String name, int version, ServiceSpec serviceSpec, String registryAuthFrom, String encodedRegistryAuth);
}
