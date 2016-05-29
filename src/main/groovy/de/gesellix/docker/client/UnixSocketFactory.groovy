package de.gesellix.docker.client

import groovy.util.logging.Slf4j
import okhttp3.Dns
import okhttp3.HttpUrl
import okio.ByteString
import org.newsclub.net.unix.AFUNIXSocket
import org.newsclub.net.unix.AFUNIXSocketAddress

import javax.net.SocketFactory

class UnixSocketFactory extends SocketFactory implements Dns {

    UnixSocketFactory() {
        if (!AFUNIXSocket.isSupported()) {
            throw new UnsupportedOperationException("AFUNIXSocket.isSupported() == false")
        }
    }

    HttpUrl urlForUnixSocketPath(String unixSocketPath, String path) {
        return new HttpUrl.Builder()
                .scheme("http")
                .host(UnixSocket.encodeHostname(unixSocketPath))
                .addPathSegment(path)
                .build()
    }

    @Override
    List<InetAddress> lookup(String hostname) throws UnknownHostException {
        return hostname.endsWith(".socket") ? [InetAddress.getByAddress(hostname, [0, 0, 0, 0] as byte[])] : SYSTEM.lookup(hostname)
    }

    @Override
    Socket createSocket() throws IOException {
        return new UnixSocket()
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
    static class UnixSocket extends Socket {

        public static final String SOCKET_MARKER = ".socket"
        private AFUNIXSocket socket

        static String encodeHostname(String path) {
            return "${Encoder.encode(path)}${SOCKET_MARKER}"
        }

        static String decodeHostname(InetAddress address) {
            String hostName = address.getHostName()
            return Encoder.decode(hostName.substring(0, hostName.indexOf(SOCKET_MARKER)))
        }

        @Override
        void connect(SocketAddress endpoint, int timeout) throws IOException {
            InetAddress address = ((InetSocketAddress) endpoint).getAddress()
            String socketPath = decodeHostname(address)

            log.debug "connect via '${socketPath}'..."
            File socketFile = new File(socketPath)

            socket = AFUNIXSocket.newInstance()
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
