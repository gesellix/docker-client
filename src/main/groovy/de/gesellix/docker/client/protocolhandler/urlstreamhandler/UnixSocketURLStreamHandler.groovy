package de.gesellix.docker.client.protocolhandler.urlstreamhandler

class UnixSocketURLStreamHandler extends URLStreamHandler {

  protected int getDefaultPort() {
    return -1
  }

  @Override
  protected URLConnection openConnection(URL u) throws IOException {
    return new HttpOverUnixSocketURLConnection(u)
  }
}
