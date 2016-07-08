package de.gesellix.docker.client

import okhttp3.Dns

import javax.net.SocketFactory

import static de.gesellix.docker.client.NamedPipeSocket.SOCKET_MARKER

class NpipeSocketFactory extends SocketFactory implements Dns {

    @Override
    List<InetAddress> lookup(String hostname) throws UnknownHostException {
        return hostname.endsWith(SOCKET_MARKER) ? [InetAddress.getByAddress(hostname, [0, 0, 0, 0] as byte[])] : SYSTEM.lookup(hostname)
    }

    @Override
    Socket createSocket() throws IOException {
        return new NamedPipeSocket()
    }

    @Override
    Socket createSocket(String s, int i) throws IOException, UnknownHostException {
        throw new UnsupportedOperationException()
    }

    @Override
    Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException, UnknownHostException {
        throw new UnsupportedOperationException()
    }

    @Override
    Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        throw new UnsupportedOperationException()
    }

    @Override
    Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
        throw new UnsupportedOperationException()
    }
}
