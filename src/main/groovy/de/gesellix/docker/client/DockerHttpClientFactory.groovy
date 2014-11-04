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

  def static createHttpClient(dockerHost) {
    HttpClientConnectionManager connectionManager

    SocketFactorySpi schemeSocketFactory = getMatchingSocketFactory(dockerHost)
    if (schemeSocketFactory) {
      dockerHost = schemeSocketFactory.sanitize(dockerHost)
      schemeSocketFactory.configureFor(dockerHost)

      def uri = new URI(dockerHost)
      def scheme = uri.scheme

      Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.create()
          .register(scheme, schemeSocketFactory)
          .build()

      connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry)
    } else {
      connectionManager = new PoolingHttpClientConnectionManager()
    }

    def newHttpClient = HttpClientBuilder.create()
        .setConnectionManager(connectionManager)
        .build()
    return newHttpClient
  }

  def static SocketFactorySpi getMatchingSocketFactory(dockerHost) {
    SocketFactoryService socketFactoryService = SocketFactoryService.getInstance()
    return socketFactoryService.getSchemeSocketFactory(dockerHost)
  }

  @Deprecated
  def static createOldHttpClient(dockerHost) {
    ClientConnectionManager connectionManager

    SocketFactorySpi schemeSocketFactory = getMatchingSocketFactory(dockerHost)
    if (schemeSocketFactory) {
      dockerHost = schemeSocketFactory.sanitize(dockerHost)
      schemeSocketFactory.configureFor(dockerHost)

      def uri = new URI(dockerHost)
      def scheme = uri.scheme

      def schemeRegistry = SchemeRegistryFactory.createSystemDefault()
      schemeRegistry.register(new Scheme(scheme, uri.port, schemeSocketFactory))

      connectionManager = new PoolingClientConnectionManager(schemeRegistry)
    } else {
      connectionManager = new PoolingClientConnectionManager()
    }

    def httpClient = new DefaultHttpClient(connectionManager);
    return httpClient
  }
}
