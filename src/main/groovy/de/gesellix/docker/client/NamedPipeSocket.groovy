package de.gesellix.docker.client

import groovy.util.logging.Slf4j

@Slf4j
class NamedPipeSocket extends Socket {

    public static final String SOCKET_MARKER = ".npipe"
    private RandomAccessFile namedPipe
    private boolean isClosed = false

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

        socketPath = socketPath.replaceAll("/", "\\\\")
        this.namedPipe = new RandomAccessFile(socketPath, "rw")
    }

    @Override
    public boolean isClosed() {
        return this.isClosed
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(this.namedPipe.getFD())
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return new FileOutputStream(this.namedPipe.getFD())
    }

    @Override
    public synchronized void close() throws IOException {
        this.namedPipe.close()
        this.isClosed = true
    }
}
