package de.gesellix.docker.client.stack.types;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class StackNetwork {

  private Map<String, String> labels = new HashMap<>();
  private String driver;
  private Map<String, String> driverOpts = new HashMap<>();
  private boolean internal;
  private boolean attachable;
  private Map<String, Object> ipam = new HashMap<>();

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }

  public String getDriver() {
    return driver;
  }

  public void setDriver(String driver) {
    this.driver = driver;
  }

  public Map<String, String> getDriverOpts() {
    return driverOpts;
  }

  public void setDriverOpts(Map<String, String> driverOpts) {
    this.driverOpts = driverOpts;
  }

  public boolean getInternal() {
    return internal;
  }

  public boolean isInternal() {
    return internal;
  }

  public void setInternal(boolean internal) {
    this.internal = internal;
  }

  public boolean getAttachable() {
    return attachable;
  }

  public boolean isAttachable() {
    return attachable;
  }

  public void setAttachable(boolean attachable) {
    this.attachable = attachable;
  }

  public Map<String, Object> getIpam() {
    return ipam;
  }

  public void setIpam(Map<String, Object> ipam) {
    this.ipam = ipam;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {return true;}
    if (o == null || getClass() != o.getClass()) {return false;}
    StackNetwork that = (StackNetwork) o;
    return internal == that.internal && attachable == that.attachable && Objects.equals(labels, that.labels) && Objects.equals(driver, that.driver) &&
           Objects.equals(driverOpts, that.driverOpts) && Objects.equals(ipam, that.ipam);
  }

  @Override
  public int hashCode() {
    return Objects.hash(labels, driver, driverOpts, internal, attachable, ipam);
  }

  @Override
  public String toString() {
    return "StackNetwork{" +
           "labels=" + labels +
           ", driver='" + driver + '\'' +
           ", driverOpts=" + driverOpts +
           ", internal=" + internal +
           ", attachable=" + attachable +
           ", ipam=" + ipam +
           '}';
  }
}
