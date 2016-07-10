package de.gesellix.docker.client.filesocket

class NpipeSocketFactory extends FileSocketFactory {

    @Override
    Socket createSocket() throws IOException {
        return new NamedPipeSocket()
    }
}
