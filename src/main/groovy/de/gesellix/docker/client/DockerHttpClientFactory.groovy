package de.gesellix.docker.client

import org.apache.http.config.Registry
import org.apache.http.config.RegistryBuilder
import org.apache.http.conn.ClientConnectionManager
import org.apache.http.conn.HttpClientConnectionManager
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.socket.ConnectionSocketFactory
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.conn.PoolingClientConnectionManager
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.impl.conn.SchemeRegistryFactory
import socketfactory.SocketFactoryService
import socketfactory.spi.SocketFactorySpi

class DockerHttpClientFactory {

  def sanitizedUri
  def SocketFactorySpi schemeSocketFactory

  DockerHttpClientFactory(dockerHost) {
    prepareSanitizedUri(dockerHost)
  }

  def prepareSanitizedUri(dockerHost) {
    if (sanitizedUri == null) {
      schemeSocketFactory = getMatchingSocketFactory(dockerHost)
      if (schemeSocketFactory) {
        sanitizedUri = schemeSocketFactory.sanitize(dockerHost)
        schemeSocketFactory.configureFor(sanitizedUri)
      }
      else {
        sanitizedUri = dockerHost
      }
    }
  }

  static def SocketFactorySpi getMatchingSocketFactory(dockerHost) {
    SocketFactoryService socketFactoryService = SocketFactoryService.getInstance()
    return socketFactoryService.getSchemeSocketFactory(dockerHost) as SocketFactorySpi
  }

  def createHttpClient() {
    HttpClientConnectionManager connectionManager = createConnectionManager()
    def newHttpClient = HttpClientBuilder.create()
        .setConnectionManager(connectionManager)
        .build()
    return newHttpClient
  }

  def createConnectionManager() {
    if (schemeSocketFactory) {
      def uri = new URI(sanitizedUri as String)
      def scheme = uri.scheme

      Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.create()
          .register(scheme, schemeSocketFactory)
          .build()

      new PoolingHttpClientConnectionManager(socketFactoryRegistry)
    }
    else {
      new PoolingHttpClientConnectionManager()
    }
  }

  @Deprecated
  def createOldHttpClient() {
    ClientConnectionManager connectionManager = createOldConnectionManager()
    def httpClient = new DefaultHttpClient(connectionManager);
    return httpClient
  }

  @Deprecated
  def createOldConnectionManager() {
    if (schemeSocketFactory) {
      def uri = new URI(sanitizedUri as String)
      def scheme = uri.scheme
      def port = 49999

      def schemeRegistry = SchemeRegistryFactory.createSystemDefault()
      schemeRegistry.register(new Scheme(scheme, port, schemeSocketFactory))

      return new PoolingClientConnectionManager(schemeRegistry)
    }
    else {
      return new PoolingClientConnectionManager()
    }
  }
}
