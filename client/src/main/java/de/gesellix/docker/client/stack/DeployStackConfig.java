package de.gesellix.docker.client.stack;

import de.gesellix.docker.client.stack.types.StackConfig;
import de.gesellix.docker.client.stack.types.StackNetwork;
import de.gesellix.docker.client.stack.types.StackSecret;
import de.gesellix.docker.client.stack.types.StackService;
import de.gesellix.docker.client.stack.types.StackVolume;

import java.util.HashMap;
import java.util.Map;

public class DeployStackConfig {

  private Map<String, StackService> services = new HashMap<>();
  private Map<String, StackNetwork> networks = new HashMap<>();
  private Map<String, StackVolume> volumes = new HashMap<>();
  private Map<String, StackSecret> secrets = new HashMap<>();
  private Map<String, StackConfig> configs = new HashMap<>();

  public Map<String, StackService> getServices() {
    return services;
  }

  public void setServices(Map<String, StackService> services) {
    this.services = services;
  }

  public Map<String, StackNetwork> getNetworks() {
    return networks;
  }

  public void setNetworks(Map<String, StackNetwork> networks) {
    this.networks = networks;
  }

  public Map<String, StackVolume> getVolumes() {
    return volumes;
  }

  public void setVolumes(Map<String, StackVolume> volumes) {
    this.volumes = volumes;
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
