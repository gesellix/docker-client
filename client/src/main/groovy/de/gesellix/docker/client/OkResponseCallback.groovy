package de.gesellix.docker.client

import groovy.util.logging.Slf4j
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import okio.Okio

@Slf4j
class OkResponseCallback implements Callback {

    OkHttpClient client
    ConnectionProvider connectionProvider
    AttachConfig attachConfig
    InputStream stdin
    Closure onResponse
    Closure onSinkClosed
    Closure onSourceConsumed

    OkResponseCallback(OkHttpClient client, ConnectionProvider connectionProvider, AttachConfig attachConfig) {
        this.client = client
        this.connectionProvider = connectionProvider
        this.attachConfig = attachConfig
        this.stdin = attachConfig.streams.stdin
        this.onResponse = attachConfig.onResponse
        this.onSinkClosed = attachConfig.onSinkClosed
        this.onSourceConsumed = attachConfig.onSourceConsumed
    }

    @Override
    void onFailure(Call call, IOException e) {
        log.error("connection failed: ${e.message}", e)
        attachConfig.onFailure(e)
    }

    void onFailure(Exception e) {
        log.error("error", e)
        attachConfig.onFailure(e)
    }

    @Override
    void onResponse(Call call, Response response) throws IOException {
        TcpUpgradeVerificator.ensureTcpUpgrade(response)

        if (attachConfig.streams.stdin != null) {
            // pass input from the client via stdin and pass it to the output stream
            // running it in an own thread allows the client to gain back control
            def stdinSource = Okio.source(attachConfig.streams.stdin)
            def writer = new Thread(new Runnable() {
                @Override
                void run() {
                    try {
                        def bufferedSink = Okio.buffer(connectionProvider.sink)
                        while (stdinSource.read(bufferedSink.buffer(), 1024) != -1) {
                            bufferedSink.flush()
                        }
                        bufferedSink.close()
                        onSinkClosed(response)
                    } catch (Exception e) {
                        onFailure(e)
                    } finally {
                        client.dispatcher().executorService().shutdown()
                    }
                }
            })
            writer.setName("stdin-writer ${call.request().url().encodedPath()}")
            writer.start()
        }

        if (attachConfig.streams.stdout != null) {
            def bufferedStdout = Okio.buffer(Okio.sink(attachConfig.streams.stdout))
            def reader = new Thread(new Runnable() {
                @Override
                void run() {
                    try {
                        while (connectionProvider.source.read(bufferedStdout.buffer(), 1024) != -1) {
                            bufferedStdout.flush()
                        }
                        bufferedStdout.flush()
                        onSourceConsumed()
                    } catch (Exception e) {
                        onFailure(e)
                    }
                }
            })
            reader.setName("stdout-reader ${call.request().url().encodedPath()}")
            reader.start()
        }
        onResponse(response)
    }
}
