package de.gesellix.docker.client.stack;

import de.gesellix.docker.client.stack.types.StackConfig;
import de.gesellix.docker.client.stack.types.StackSecret;
import de.gesellix.docker.remote.api.NetworkCreateRequest;
import de.gesellix.docker.remote.api.ServiceSpec;

import java.util.HashMap;
import java.util.Map;

public class DeployStackConfig {

  private Map<String, ServiceSpec> services = new HashMap<>();
  private Map<String, NetworkCreateRequest> networks = new HashMap<>();
  private Map<String, StackSecret> secrets = new HashMap<>();
  private Map<String, StackConfig> configs = new HashMap<>();

  public Map<String, ServiceSpec> getServices() {
    return services;
  }

  public void setServices(Map<String, ServiceSpec> services) {
    this.services = services;
  }

  public Map<String, NetworkCreateRequest> getNetworks() {
    return networks;
  }

  public void setNetworks(Map<String, NetworkCreateRequest> networks) {
    this.networks = networks;
  }

  public Map<String, StackSecret> getSecrets() {
    return secrets;
  }

  public void setSecrets(Map<String, StackSecret> secrets) {
    this.secrets = secrets;
  }

  public Map<String, StackConfig> getConfigs() {
    return configs;
  }

  public void setConfigs(Map<String, StackConfig> configs) {
    this.configs = configs;
  }
}
