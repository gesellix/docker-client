package de.gesellix.docker.client

import org.apache.http.conn.ConnectTimeoutException
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.scheme.SchemeSocketFactory
import org.apache.http.params.HttpConnectionParams
import org.apache.http.params.HttpParams
import org.newsclub.net.unix.AFUNIXSocket
import org.newsclub.net.unix.AFUNIXSocketAddress

class UnixSocketFactory implements SchemeSocketFactory {

  File socketFile

  def static sanitize(dockerHost) {
    dockerHost.replaceAll("^unix://", "unix://localhost")
  }

  def static configure(httpClient, dockerHost) {
    def socketFilename = dockerHost.replaceAll("unix://localhost", "")
    def unixScheme = new Scheme("unix", 0xffff, new UnixSocketFactory(socketFilename))
    httpClient.getConnectionManager().getSchemeRegistry().register(unixScheme)
  }

  private UnixSocketFactory(String socketFilename) {
    socketFile = new File(socketFilename)
  }

  @Override
  Socket createSocket(HttpParams params) throws IOException {
    AFUNIXSocket socket = AFUNIXSocket.newInstance();
    return socket
  }

  @Override
  Socket connectSocket(Socket socket, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
    int connTimeout = HttpConnectionParams.getConnectionTimeout(params)
//    int soTimeout = HttpConnectionParams.getSoTimeout(params)

    try {
//      socket.setSoTimeout(soTimeout)
      socket.connect(new AFUNIXSocketAddress(socketFile), connTimeout)
//      socket.connect(new AFUNIXSocketAddress(socketFile))
    }
    catch (SocketTimeoutException e) {
      throw new ConnectTimeoutException("Connect to '" + socketFile + "' timed out")
    }

    return socket
  }

  @Override
  boolean isSecure(Socket sock) throws IllegalArgumentException {
    return false
  }
}
