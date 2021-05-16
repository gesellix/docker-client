package de.gesellix.docker.client.testutil;

import de.gesellix.docker.client.TypeSafeDockerClientImpl;

import java.util.Objects;

public class TestImage {

  private final TypeSafeDockerClientImpl dockerClient;
  private final String repository;
  private final String tag;

  public TestImage(TypeSafeDockerClientImpl dockerClient) {
    this.dockerClient = dockerClient;

    boolean isWindows = Objects.requireNonNull(dockerClient.getSystemApi().systemVersion().getOs()).equalsIgnoreCase("windows");
    this.repository = "gesellix/echo-server";
    this.tag = isWindows ? "os-windows" : "os-linux";

    // TODO consider NOT calling prepare inside the constructor
    prepare();
  }

  public void prepare() {
    dockerClient.getImageApi().imageCreate(getImageName(), null, null, getImageTag(), null, null, null, null, null);
  }

  public String getImageWithTag() {
    return getImageName() + ":" + getImageTag();
  }

  public String getImageName() {
    return repository;
  }

  public String getImageTag() {
    return tag;
  }
}
