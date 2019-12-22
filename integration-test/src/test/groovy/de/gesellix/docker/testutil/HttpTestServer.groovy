package de.gesellix.docker.testutil

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpsConfigurator
import com.sun.net.httpserver.HttpsServer
import okio.Okio

import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import java.security.KeyStore
import java.security.SecureRandom
import java.util.concurrent.Executors

class HttpTestServer {

    HttpServer httpServer
    SSLContext sslContext

    HttpTestServer(SSLContext sslContext = null) {
        this.sslContext = sslContext
    }

    InetSocketAddress start() {
        return start('/test/', new ReverseHandler())
    }

    InetSocketAddress start(String context, HttpHandler handler) {
        InetSocketAddress address = new InetSocketAddress(0)

        if (sslContext) {
            // using the VM param `-Djavax.net.debug=all` helps debugging SSL issues
            httpServer = HttpsServer.create(address, address.port)
            httpServer.setHttpsConfigurator(new DefaultHttpsConfigurator(sslContext))
        }
        else {
            httpServer = HttpServer.create(address, address.port)
        }

        httpServer.with {
            createContext(context, handler)
            setExecutor(Executors.newCachedThreadPool())
            start()
        }
        return httpServer.address
    }

    def stop() {
        if (httpServer) {
            httpServer.stop(0)
        }
    }

    /**
     Given your KeyStore is located in the classpath at "/de/gesellix/docker/testutil/test.jks",
     you may call the method like in this example:

     <pre>
     <code>
     SSLContext ctx = HttpTestServer.createDefaultSSLContext("/de/gesellix/docker/testutil/test.jks", "changeit")
     </code>
     </pre>

     If you need a new KeyStore from scratch you can use this command:

     <pre>
     <code>
     keytool -genkey -alias alias -keypass changeit -keystore test.jks -storepass changeit
     </code>
     </pre>

     Please note that you should enter a valid domain (e.g. "localhost")
     when being asked for your first and last name ("CN").
     */
    static SSLContext createDefaultSSLContext(String jksResource, String jksPassword) {
        InputStream jksInputStream = getClass().getResourceAsStream(jksResource)
        char[] password = jksPassword.toCharArray()
        KeyStore ks = KeyStore.getInstance("JKS")
        ks.load(jksInputStream, password)

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.defaultAlgorithm)
        kmf.init(ks, password)

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.defaultAlgorithm)
        tmf.init(ks)

        SSLContext sslContext = SSLContext.getInstance("TLS")
        sslContext.init(kmf.keyManagers, tmf.trustManagers, new SecureRandom())
        sslContext
    }

    static class DefaultHttpsConfigurator extends HttpsConfigurator {

        DefaultHttpsConfigurator(SSLContext sslContext) {
            super(sslContext)
        }
    }

    static class ReverseHandler implements HttpHandler {

        @Override
        void handle(HttpExchange httpExchange) {
            if (httpExchange.requestMethod == 'GET') {
                httpExchange.responseHeaders.set('Content-Type', 'text/plain')
                final String query = httpExchange.requestURI.rawQuery

                if (!query || !query.contains('string')) {
                    httpExchange.sendResponseHeaders(400, 0)
                    return
                }

                final String[] param = query.split('=')
                assert param.length == 2 && param[0] == 'string'

                httpExchange.sendResponseHeaders(200, 0)
                httpExchange.responseBody.write(param[1].reverse().bytes)
                httpExchange.responseBody.close()
            }
        }
    }

    static class RecordingRequestsHandler implements HttpHandler {

        List<String> recordedRequests = []
        Map<String, Map<String, List<String>>> recordedHeadersByRequest = [:]

        @Override
        void handle(HttpExchange httpExchange) {
            String request = "${httpExchange.requestMethod} ${httpExchange.requestURI}"
            recordedRequests << request
            recordedHeadersByRequest[request] = httpExchange.requestHeaders

            httpExchange.sendResponseHeaders(200, 0)
        }
    }

    static class FileServer implements HttpHandler {

        URL file

        FileServer(URL file) {
            this.file = file
        }

        @Override
        void handle(HttpExchange httpExchange) {
            if (httpExchange.requestMethod == 'GET') {
                httpExchange.sendResponseHeaders(200, 0)
                httpExchange.responseBody.write(toString((file as URL).newInputStream()).bytes)
                httpExchange.responseBody.close()
            }
        }

        private String toString(InputStream source) {
            Okio.buffer(Okio.source(source)).readUtf8()
        }
    }
}
