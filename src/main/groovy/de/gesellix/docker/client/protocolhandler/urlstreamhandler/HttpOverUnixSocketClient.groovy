package de.gesellix.docker.client.protocolhandler.urlstreamhandler

import org.newsclub.net.unix.AFUNIXSocket
import org.newsclub.net.unix.AFUNIXSocketAddress
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sun.net.www.http.HttpClient

// behave like the sun internal HttpClient,
// but connect via unix socket to the docker daemon.
class HttpOverUnixSocketClient extends HttpClient {

    final static Logger logger = LoggerFactory.getLogger(HttpOverUnixSocketClient)

    static String dockerUnixSocket

    protected HttpOverUnixSocketClient(URL url) throws IOException {
        super(url, true)
    }

    @Override
    protected Socket doConnect(String host, int port) throws IOException, UnknownHostException {
        logger.debug "connect via '${dockerUnixSocket}'..."

        File socketFile = new File(dockerUnixSocket)
        logger.debug "unix socket exists/canRead/canWrite: ${socketFile.exists()}/${socketFile.canRead()}/${socketFile.canWrite()}"

        Socket socket = AFUNIXSocket.newInstance()

        if (connectTimeout < 0) {
            connectTimeout = defaultConnectTimeout
        }
        socket.connect(new AFUNIXSocketAddress(socketFile), connectTimeout)

        if (readTimeout < 0) {
            readTimeout = defaultSoTimeout
        }
        if (this.readTimeout >= 0) {
            socket.setSoTimeout(defaultSoTimeout)
        }
        return socket
    }
}
