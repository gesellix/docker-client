package de.gesellix.docker.client.protocolhandler.urlstreamhandler

import org.newsclub.net.unix.AFUNIXSocket
import org.newsclub.net.unix.AFUNIXSocketAddress
import sun.net.www.http.HttpClient

class HttpOverUnixSocketClient extends HttpClient {

  static String dockerUnixSocket

  protected HttpOverUnixSocketClient(URL url) throws IOException {
    super(url, true)
  }

  @Override
  protected Socket doConnect(String host, int port) throws IOException, UnknownHostException {
    File socketFile = new File(dockerUnixSocket)

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
