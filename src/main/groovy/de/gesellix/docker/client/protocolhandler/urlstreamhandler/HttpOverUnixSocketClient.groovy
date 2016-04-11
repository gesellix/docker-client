package de.gesellix.docker.client.protocolhandler.urlstreamhandler

import groovy.util.logging.Slf4j
import org.newsclub.net.unix.AFUNIXSocket
import org.newsclub.net.unix.AFUNIXSocketAddress
import sun.net.www.http.HttpClient

// behave like the sun internal HttpClient,
// but connect via unix socket to the docker daemon.
@Slf4j
class HttpOverUnixSocketClient extends HttpClient {

    protected HttpOverUnixSocketClient(URL url) throws IOException {
        super(url, true)
    }

    @Override
    protected Socket doConnect(String host, int port) throws IOException, UnknownHostException {
        host = URLDecoder.decode(host, "UTF-8")
        log.debug "connect via '${host}'..."

        File socketFile = new File(host)
        log.debug "unix socket exists/canRead/canWrite: ${socketFile.exists()}/${socketFile.canRead()}/${socketFile.canWrite()}"

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
