package de.gesellix.docker.client

import de.gesellix.docker.engine.EngineResponse
import de.gesellix.docker.response.JsonChunksReader
import de.gesellix.docker.response.LineReader
import de.gesellix.docker.response.Reader
import groovy.util.logging.Slf4j
import okio.Okio
import okio.Source

@Slf4j
class DockerAsyncConsumer implements Runnable {

  private EngineResponse response
  private DockerAsyncCallback callback

  DockerAsyncConsumer(EngineResponse response, DockerAsyncCallback callback) {
    this.response = response
    this.callback = callback
  }

  @Override
  void run() {
    try {
      Reader reader = createReader(response)
      while (reader.hasNext()) {
        def chunk = reader.readNext()
        log.trace("event: $chunk")
        callback.onEvent(chunk)
      }
    }
    catch (InterruptedException | InterruptedIOException e) {
      log.debug("consumer interrupted", e)
      Thread.currentThread().interrupt()
    }
    catch (Exception e) {
      log.error("error reading from stream", e)
      throw new RuntimeException(e)
    }
    finally {
      callback?.onFinish()
      response.stream?.close()
    }
  }

  private Reader createReader(EngineResponse response) {
    Source source = Okio.source(response.stream as InputStream)
    if (response.contentType == "application/json"
        && response.headers?.get("transfer-encoding") == "chunked") {
      return new JsonChunksReader(Okio.source(response.stream as InputStream))
    }
    else {
      return new LineReader(source)
    }
  }
}
