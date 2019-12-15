package de.gesellix.docker.testutil

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import de.gesellix.docker.client.Timeout
import groovy.util.logging.Slf4j
import okio.Okio

@Slf4j
class ChunkedStreamServer implements HttpHandler {

    List<String> chunks
    private Timeout pauseBetweenChunks

    ChunkedStreamServer(List<String> chunks, Timeout pauseBetweenChunks) {
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
                Thread.sleep(pauseBetweenChunks.unit.toMillis(pauseBetweenChunks.timeout))
            }
            httpExchange.responseBody.close()
        }
    }

    private String toString(InputStream source) {
        Okio.buffer(Okio.source(source)).readUtf8()
    }
}
