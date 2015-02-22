package de.gesellix.docker.client.protocolhandler

import de.gesellix.docker.client.protocolhandler.content.application.json
import de.gesellix.docker.client.protocolhandler.content.application.vnd_docker_raw_stream
import sun.net.www.content.text.plain

class DockerContentHandlerFactory implements ContentHandlerFactory {

  @Override
  ContentHandler createContentHandler(String contentType) {
    def mimetype = contentType.replace(" ", "").split(";").first()
    switch (mimetype) {
      case "text/plain":
      case "text/html":
        return new plain()
      case "application/json":
        return new json()
      case "application/vnd.docker.raw-stream":
        return new vnd_docker_raw_stream()
    }
    return null
  }
}
