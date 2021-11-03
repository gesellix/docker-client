package de.gesellix.docker.client.stack.types;

import de.gesellix.docker.remote.api.TaskSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StackService {

  private String name;
  private Map<Object, Object> labels = new HashMap<>();
  private Map<Object, Object> endpointSpec = new HashMap<>();
  private Map<Object, Object> mode = new HashMap<>();
  private TaskSpec taskTemplate = new TaskSpec();
  private Map<Object, Object> updateConfig = new HashMap<>();
  private List<Map<String,Object>> networks = new ArrayList<>();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Map<Object, Object> getLabels() {
    return labels;
  }

  public void setLabels(Map<Object, Object> labels) {
    this.labels = labels;
  }

  public Map<Object, Object> getEndpointSpec() {
    return endpointSpec;
  }

  public void setEndpointSpec(Map<Object, Object> endpointSpec) {
    this.endpointSpec = endpointSpec;
  }

  public Map<Object, Object> getMode() {
    return mode;
  }

  public void setMode(Map<Object, Object> mode) {
    this.mode = mode;
  }

  public TaskSpec getTaskTemplate() {
    return taskTemplate;
  }

  public void setTaskTemplate(TaskSpec taskTemplate) {
    this.taskTemplate = taskTemplate;
  }

  public Map<Object, Object> getUpdateConfig() {
    return updateConfig;
  }

  public void setUpdateConfig(Map<Object, Object> updateConfig) {
    this.updateConfig = updateConfig;
  }

  public List<Map<String, Object>> getNetworks() {
    return networks;
  }

  public void setNetworks(List<Map<String, Object>> networks) {
    this.networks = networks;
  }

  @Override
  public String toString() {
    return "StackService{" +
           "name='" + name + '\'' +
           ", labels=" + labels +
           ", endpointSpec=" + endpointSpec +
           ", mode=" + mode +
           ", taskTemplate=" + taskTemplate +
           ", updateConfig=" + updateConfig +
           ", networks=" + networks +
           '}';
  }
}
