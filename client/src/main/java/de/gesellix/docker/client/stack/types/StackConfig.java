package de.gesellix.docker.client.stack.types;

import de.gesellix.docker.remote.api.Driver;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class StackConfig {

  private String name;
  private byte[] data;
  private Map<String, String> labels = new HashMap<>();
  private Driver driver;
  private Driver templating;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }

  public Driver getDriver() {
    return driver;
  }

  public void setDriver(Driver driver) {
    this.driver = driver;
  }

  public Driver getTemplating() {
    return templating;
  }

  public void setTemplating(Driver templating) {
    this.templating = templating;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {return true;}
    if (o == null || getClass() != o.getClass()) {return false;}
    StackConfig that = (StackConfig) o;
    return Objects.equals(name, that.name) && Arrays.equals(data, that.data) && Objects.equals(labels, that.labels) && Objects.equals(driver, that.driver) &&
           Objects.equals(templating, that.templating);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(name, labels, driver, templating);
    result = 31 * result + Arrays.hashCode(data);
    return result;
  }

  @Override
  public String toString() {
    return "StackConfig{" +
           "name='" + name + '\'' +
           ", labels=" + labels +
           ", driver=" + driver +
           ", templating=" + templating +
           '}';
  }
}
