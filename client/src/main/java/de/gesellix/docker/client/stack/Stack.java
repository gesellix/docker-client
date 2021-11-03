package de.gesellix.docker.client.stack;

import java.util.Objects;

public class Stack {

  private String name;
  private int services;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getServices() {
    return services;
  }

  public void setServices(int services) {
    this.services = services;
  }

  @Override
  public String toString() {
    return getName() + ": " + getServices();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {return true;}
    if (o == null || getClass() != o.getClass()) {return false;}
    Stack stack = (Stack) o;
    return services == stack.services && Objects.equals(name, stack.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, services);
  }
}
