package de.gesellix.docker.client.protocolhandler.content.application

class octet_stream extends ContentHandler {

    @Override
    Object getContent(URLConnection connection) throws IOException {
        return connection.inputStream
    }
}
