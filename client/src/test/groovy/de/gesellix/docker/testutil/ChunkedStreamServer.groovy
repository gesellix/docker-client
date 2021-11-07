package de.gesellix.docker.testutil

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import groovy.util.logging.Slf4j

import java.time.Duration

@Slf4j
class ChunkedStreamServer implements HttpHandler {

  List<String> chunks
  private Duration pauseBetweenChunks

  ChunkedStreamServer(List<String> chunks, Duration pauseBetweenChunks) {
    this.chunks = chunks
    this.pauseBetweenChunks = pauseBetweenChunks
  }

  @Override
  void handle(HttpExchange httpExchange) {
    if (httpExchange.requestMethod == 'GET') {
      httpExchange.responseHeaders.set("Transfer-Encoding", "chunked")
      httpExchange.sendResponseHeaders(200, 0)

      chunks.each { chunk ->
        log.info("sending chunk...")
        httpExchange.responseBody.write(chunk.bytes)
        httpExchange.responseBody.flush()
        log.info("pausing chunk...")
        Thread.sleep(pauseBetweenChunks.toMillis())
      }
      httpExchange.responseBody.close()
    }
  }
}
