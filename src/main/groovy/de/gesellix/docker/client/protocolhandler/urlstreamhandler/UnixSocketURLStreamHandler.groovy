package de.gesellix.docker.client.protocolhandler.urlstreamhandler

/**
 * This is class marked as abstract, because it is normally
 * instantiated by the sun.net.www.protocol.unix.Handler subclass.
 * @see sun.net.www.protocol.unix.Handler
 * @see URLStreamHandler
 */
abstract class UnixSocketURLStreamHandler extends URLStreamHandler {

  @Override
  protected URLConnection openConnection(URL u) throws IOException {
    return new HttpOverUnixSocketURLConnection(u)
  }

  @Override
  protected URLConnection openConnection(URL u, Proxy p) throws IOException {
    // ignore the proxy
    return this.openConnection(u)
  }
}
