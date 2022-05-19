package de.gesellix.docker.client;

import de.gesellix.docker.engine.EngineResponse;

public class DockerClientException extends RuntimeException {

  private final EngineResponse detail;

  public DockerClientException(Throwable cause) {
    this(cause, null);
  }

  public DockerClientException(Throwable cause, EngineResponse detail) {
    super(cause);
    this.detail = detail;
  }

  public EngineResponse getDetail() {
    return detail;
  }

  @Override
  public String toString() {
    return "DockerClientException{...} " + super.toString();
  }
}
