package de.gesellix.docker.client

import de.gesellix.docker.engine.EngineResponse
import groovy.util.logging.Slf4j

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
        def reader = new BufferedReader(new InputStreamReader(response.stream as InputStream))
        try {
            String line
            while ((line = reader.readLine()) != null) {
                log.trace("event: $line")
                callback.onEvent(line)
            }
        }
        catch (Exception e) {
            log.error("error reading from stream", e)
            throw new RuntimeException(e)
        }
        finally {
            callback?.onFinish()
        }
    }
}
