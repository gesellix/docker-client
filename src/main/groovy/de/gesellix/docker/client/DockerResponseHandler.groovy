package de.gesellix.docker.client

import org.codehaus.groovy.runtime.MethodClosure

class DockerResponseHandler {

  def ensureSuccessfulResponse(def response, Throwable context) {
    if (!response || !response.status.success || response.content?.error) {
      throw new DockerClientException(context, response?.content);
    }
  }

  @Deprecated
  Map<String, Closure> contentTypeReaders() {
    return [
        "application/vnd.docker.raw-stream": new MethodClosure(this, "readRawDockerStream"),
        "application/x-tar"                : new MethodClosure(this, "readTarStream"),
        "application/json"                 : new MethodClosure(this, "readJson"),
        "text/plain"                       : new MethodClosure(this, "readText"),
        "text/html"                        : new MethodClosure(this, "readText"),
        "*/*"                              : new MethodClosure(this, "readText")
    ]
  }
}
