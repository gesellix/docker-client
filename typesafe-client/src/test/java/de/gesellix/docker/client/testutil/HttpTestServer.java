package de.gesellix.docker.client.testutil;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import okio.BufferedSink;
import okio.Okio;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class HttpTestServer {

  private HttpServer httpServer;
  private final SSLContext sslContext;

  public HttpTestServer() {
    this(null);
  }

  public HttpTestServer(SSLContext sslContext) {
    this.sslContext = sslContext;
  }

  public InetSocketAddress start() throws IOException {
    return start("/test/", new ReverseHandler());
  }

  public InetSocketAddress start(final String context, final HttpHandler handler) throws IOException {
    InetSocketAddress address = new InetSocketAddress(0);

    if (sslContext != null) {
      // using the VM param `-Djavax.net.debug=all` helps debugging SSL issues
      httpServer = HttpsServer.create(address, address.getPort());
      ((HttpsServer) httpServer).setHttpsConfigurator(new DefaultHttpsConfigurator(sslContext));
    }
    else {
      httpServer = HttpServer.create(address, address.getPort());
    }

    httpServer.createContext(context, handler);
    httpServer.setExecutor(Executors.newCachedThreadPool());
    httpServer.start();
    return httpServer.getAddress();
  }

  public void stop() {
    if (httpServer != null) {
      httpServer.stop(0);
    }
  }

  /**
   * Given your KeyStore is located in the classpath at "/de/gesellix/docker/testutil/test.jks",
   * you may call the method like in this example:
   *
   * <pre>
   * <code>
   * SSLContext ctx = HttpTestServer.createDefaultSSLContext("/de/gesellix/docker/testutil/test.jks", "changeit")
   * </code>
   * </pre>
   *
   * If you need a new KeyStore from scratch you can use this command:
   *
   * <pre>
   * <code>
   * keytool -genkey -alias alias -keypass changeit -keystore test.jks -storepass changeit
   * </code>
   * </pre>
   *
   * Please note that you should enter a valid domain (e.g. "localhost")
   * when being asked for your first and last name ("CN").
   */
  public static SSLContext createDefaultSSLContext(String jksResource, String jksPassword) throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException, UnrecoverableKeyException, KeyManagementException {
    InputStream jksInputStream = HttpTestServer.class.getResourceAsStream(jksResource);
    char[] password = jksPassword.toCharArray();
    KeyStore ks = KeyStore.getInstance("JKS");
    ks.load(jksInputStream, password);

    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(ks, password);

    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(ks);

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
    return sslContext;
  }

  public static class DefaultHttpsConfigurator extends HttpsConfigurator {

    public DefaultHttpsConfigurator(SSLContext sslContext) {
      super(sslContext);
    }
  }

  public static class ReverseHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
      if (httpExchange.getRequestMethod().equals("GET")) {
        httpExchange.getResponseHeaders().set("Content-Type", "text/plain");
        final String query = httpExchange.getRequestURI().getRawQuery();

        if (query == null || !query.contains("string")) {
          httpExchange.sendResponseHeaders(400, 0);
          return;
        }

        final String[] param = query.split("=");
        assert param.length == 2 && param[0].equals("string");

        httpExchange.sendResponseHeaders(200, 0);
        httpExchange.getResponseBody().write(new StringBuilder(param[1]).reverse().toString().getBytes());
        httpExchange.getResponseBody().close();
      }
    }
  }

  public static class RecordingRequestsHandler implements HttpHandler {

    private final List<String> recordedRequests = new ArrayList<>();
    private final Map<String, Map<String, List<String>>> recordedHeadersByRequest = new HashMap<>();

    @Override
    public void handle(final HttpExchange httpExchange) throws IOException {
      String request = httpExchange.getRequestMethod() + " " + httpExchange.getRequestURI();
      recordedRequests.add(request);
      recordedHeadersByRequest.put(request, httpExchange.getRequestHeaders());

      httpExchange.sendResponseHeaders(200, 0);
    }

    public List<String> getRecordedRequests() {
      return recordedRequests;
    }

    public Map<String, Map<String, List<String>>> getRecordedHeadersByRequest() {
      return recordedHeadersByRequest;
    }
  }

  public static class FileServer implements HttpHandler {

    private final URL file;

    public FileServer(URL file) {
      this.file = file;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
      if (httpExchange.getRequestMethod().equals("GET")) {
        httpExchange.sendResponseHeaders(200, 0);
        copy(file.openStream(), httpExchange.getResponseBody());
        httpExchange.getResponseBody().close();
      }
    }

    private void copy(InputStream source, OutputStream sink) throws IOException {
      BufferedSink bufferedSink = Okio.buffer(Okio.sink(sink));
      bufferedSink.writeAll(Okio.buffer(Okio.source(source)));
      bufferedSink.flush();
    }
  }
}
