package de.gesellix.docker.client

import com.squareup.moshi.JsonReader
import de.gesellix.docker.engine.EngineResponse
import groovy.util.logging.Slf4j
import okio.BufferedSource
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

    static interface Reader {

        Object readNext()

        boolean hasNext()
    }

    static class LineReader implements Reader {

        private BufferedSource buffer

        LineReader(Source source) {
            this.buffer = Okio.buffer(source)
        }

        Object readNext() {
            return buffer.readUtf8Line()
        }

        boolean hasNext() {
            !Thread.currentThread().isInterrupted() && !buffer.exhausted()
        }
    }

    static class JsonChunksReader implements Reader {

        JsonReader reader

        JsonChunksReader(Source source) {
            reader = JsonReader.of(Okio.buffer(source))

            // for transfer-encoding: chunked
            reader.setLenient(true)
        }

        Object readNext() {
//            return JsonOutput.toJson(reader.readJsonValue())
            return reader.readJsonValue()
        }

        boolean hasNext() {
            return !Thread.currentThread().isInterrupted() && reader.hasNext()
        }
    }
}
