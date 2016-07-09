package de.gesellix.docker.client

class NpipeSocketFactory extends FileSocketFactory {

    @Override
    Socket createSocket() throws IOException {
        return new NamedPipeSocket()
    }
}
