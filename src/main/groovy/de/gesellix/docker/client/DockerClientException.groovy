package de.gesellix.docker.client

class DockerClientException extends RuntimeException {

  def detail

  DockerClientException(Throwable cause, detail = [:]) {
    super(cause)
    this.detail = detail
  }
}
