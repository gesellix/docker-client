package de.gesellix.docker.client

import groovy.util.logging.Slf4j
import okhttp3.Dns
import okhttp3.HttpUrl
import okio.ByteString

import javax.net.SocketFactory

class NpipeSocketFactory extends SocketFactory implements Dns {

    HttpUrl urlForNamedPipeSocketPath(String namedPipePath, String path) {
        return new HttpUrl.Builder()
                .scheme("http")
                .host(NamedPipeSocket.encodeHostname(namedPipePath))
                .addPathSegment(path)
                .build()
    }

    @Override
    List<InetAddress> lookup(String hostname) throws UnknownHostException {
        return hostname.endsWith(".npipe") ? [InetAddress.getByAddress(hostname, [0, 0, 0, 0] as byte[])] : SYSTEM.lookup(hostname)
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

    @Slf4j
    class NamedPipeSocket extends Socket {

        public static final String SOCKET_MARKER = ".npipe"

        private RandomAccessFile namedPipe
        private boolean isClosed = false

        static String encodeHostname(String path) {
            return "${Encoder.encode(path)}${SOCKET_MARKER}"
        }

        static String decodeHostname(InetAddress address) {
            String hostName = address.getHostName()
            return Encoder.decode(hostName.substring(0, hostName.indexOf(SOCKET_MARKER)))
        }

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

    private static class Encoder {

        static String encode(String toEncode) {
            return ByteString.encodeUtf8(toEncode).hex()
        }

        static String decode(String toDecode) {
            return ByteString.decodeHex(toDecode).utf8()
        }
    }
}
