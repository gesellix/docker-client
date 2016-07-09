package de.gesellix.docker.client

import groovy.util.logging.Slf4j
import org.newsclub.net.unix.AFUNIXSocket
import org.newsclub.net.unix.AFUNIXSocketAddress

@Slf4j
class UnixSocket extends FileSocket {

    private AFUNIXSocket socket

    @Override
    void connect(SocketAddress endpoint, int timeout) throws IOException {
        InetAddress address = ((InetSocketAddress) endpoint).getAddress()
        String socketPath = decodeHostname(address)

        log.debug "connect via '${socketPath}'..."
        File socketFile = new File(socketPath)

        socket = AFUNIXSocket.newInstance()

        if (timeout < 0) {
            timeout = 0
        }
        socket.connect(new AFUNIXSocketAddress(socketFile), timeout)
        socket.setSoTimeout(timeout)
    }

    @Override
    InputStream getInputStream() throws IOException {
        return socket.getInputStream()
    }

    @Override
    OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream()
    }

    @Override
    void bind(SocketAddress bindpoint) throws IOException {
        socket.bind(bindpoint)
    }

    @Override
    boolean isConnected() {
        return socket.isConnected()
    }

    @Override
    synchronized void close() throws IOException {
        socket.close()
    }
}
