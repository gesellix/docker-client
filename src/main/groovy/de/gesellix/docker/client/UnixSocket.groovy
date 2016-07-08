package de.gesellix.docker.client

import groovy.util.logging.Slf4j
import org.newsclub.net.unix.AFUNIXSocket
import org.newsclub.net.unix.AFUNIXSocketAddress

@Slf4j
class UnixSocket extends Socket {

    static final String SOCKET_MARKER = ".socket"
    private AFUNIXSocket socket

    static String encodeHostname(String path) {
        return "${HostnameEncoder.encode(path)}${SOCKET_MARKER}"
    }

    static String decodeHostname(InetAddress address) {
        String hostName = address.getHostName()
        return HostnameEncoder.decode(hostName.substring(0, hostName.indexOf(SOCKET_MARKER)))
    }

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
    boolean isConnected() {
        return socket.isConnected()
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
    void close() throws IOException {
        socket.close()
    }
}
