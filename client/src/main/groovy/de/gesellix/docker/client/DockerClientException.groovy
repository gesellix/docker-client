package de.gesellix.docker.client

import de.gesellix.docker.engine.EngineResponse

class DockerClientException extends RuntimeException {

  EngineResponse detail

  DockerClientException(Throwable cause) {
    this(cause, null)
  }

  DockerClientException(Throwable cause, EngineResponse detail) {
    super(cause)
    this.detail = detail
  }

  @Override
  String toString() {
    return "DockerClientException{...} ${super.toString()}"
  }
}
