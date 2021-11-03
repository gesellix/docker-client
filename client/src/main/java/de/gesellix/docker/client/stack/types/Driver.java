package de.gesellix.docker.client.stack.types;

import java.util.Map;
import java.util.Objects;

public class Driver {

  private String name;
  private Map<String, String> options;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Map<String, String> getOptions() {
    return options;
  }

  public void setOptions(Map<String, String> options) {
    this.options = options;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {return true;}
    if (o == null || getClass() != o.getClass()) {return false;}
    Driver driver = (Driver) o;
    return Objects.equals(name, driver.name) && Objects.equals(options, driver.options);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, options);
  }

  @Override
  public String toString() {
    return "Driver{" +
           "name='" + name + '\'' +
           ", options=" + options +
           '}';
  }
}
