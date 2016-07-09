package de.gesellix.docker.client

import groovy.util.logging.Slf4j

@Slf4j
class NamedPipeSocket extends FileSocket {

    private RandomAccessFile namedPipe
    private boolean isClosed = false

    @Override
    void connect(SocketAddress endpoint, int timeout) throws IOException {
        InetAddress address = ((InetSocketAddress) endpoint).getAddress()
        String socketPath = decodeHostname(address)

        log.debug "connect via '${socketPath}'..."

        socketPath = socketPath.replaceAll("/", "\\\\")
        this.namedPipe = new RandomAccessFile(socketPath, "rw")
    }

    @Override
    InputStream getInputStream() throws IOException {
        return new FileInputStream(this.namedPipe.getFD())
    }

    @Override
    OutputStream getOutputStream() throws IOException {
        return new FileOutputStream(this.namedPipe.getFD())
    }

    @Override
    boolean isClosed() {
        return this.isClosed
    }

    @Override
    synchronized void close() throws IOException {
        this.namedPipe.close()
        this.isClosed = true
    }
}
