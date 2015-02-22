package de.gesellix.docker.client.protocolhandler.content.application

import de.gesellix.docker.client.protocolhandler.RawInputStream

class vnd_docker_raw_stream extends ContentHandler {

  @Override
  Object getContent(URLConnection connection) throws IOException {
    return new RawInputStream(connection.inputStream)
  }
}
