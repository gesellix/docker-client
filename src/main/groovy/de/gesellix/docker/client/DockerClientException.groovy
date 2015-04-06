package de.gesellix.docker.client

class DockerClientException extends RuntimeException {

  def detail

  DockerClientException(Throwable cause, detail = [:]) {
    super(cause)
    this.detail = detail
  }

  @Override
  public String toString() {
    if (detail instanceof String) {
      return "DockerClientException{detail=$detail} ${super.toString()}"
    }
    else {
      return "DockerClientException{...} ${super.toString()}"
    }
  }
}
