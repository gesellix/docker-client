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
    Closure onStdinClosed
    Closure onFinish

    OkResponseCallback(OkHttpClient client, ConnectionProvider connectionProvider, AttachConfig attachConfig) {
        this.client = client
        this.connectionProvider = connectionProvider
        this.attachConfig = attachConfig
        this.stdin = attachConfig.streams.stdin
        this.onResponse = attachConfig.onResponse
        this.onStdinClosed = attachConfig.onStdinClosed
        this.onFinish = attachConfig.onFinish
    }

    @Override
    void onFailure(Call call, IOException e) {
        log.error "connection failed: ${e.message}"
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
                public void run() {
                    try {
                        def bufferedSink = Okio.buffer(connectionProvider.sink)
                        while (stdinSource.read(bufferedSink.buffer(), 1024) != -1) {
                            bufferedSink.flush()
                        }
                        Thread.sleep(100)
                        bufferedSink.close()
                        onStdinClosed(response)
                    } catch (Exception e) {
                        log.error("error", e)
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
                public void run() {
                    try {
                        while (connectionProvider.source.read(bufferedStdout.buffer(), 1024) != -1) {
                            bufferedStdout.flush()
                        }
                    } catch (Exception e) {
                        log.error("error", e)
                    }
                    onFinish()
                }
            })
            reader.setName("stdout-reader ${call.request().url().encodedPath()}")
            reader.start()
        }
        onResponse(response)
    }
}
