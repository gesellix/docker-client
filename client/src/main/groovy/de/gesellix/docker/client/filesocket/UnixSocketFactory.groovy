package de.gesellix.docker.client.filesocket

import org.newsclub.net.unix.AFUNIXSocket

class UnixSocketFactory extends FileSocketFactory {

    UnixSocketFactory() {
        if (!AFUNIXSocket.isSupported()) {
            throw new UnsupportedOperationException("AFUNIXSocket.isSupported() == false")
        }
    }

    @Override
    Socket createSocket() throws IOException {
        return new UnixSocket()
    }
}
