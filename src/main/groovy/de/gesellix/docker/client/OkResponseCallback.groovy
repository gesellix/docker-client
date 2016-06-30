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
    Closure done

    OkResponseCallback(OkHttpClient client, ConnectionProvider connectionProvider, AttachConfig attachConfig) {
        this.client = client
        this.connectionProvider = connectionProvider
        this.attachConfig = attachConfig
        this.stdin = attachConfig.streams.stdin
        this.onResponse = attachConfig.onResponse
        this.done = attachConfig.onStdinClosed
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
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        def bufferedSink = Okio.buffer(connectionProvider.sink)
                        def stdinSource = Okio.source(attachConfig.streams.stdin)
                        while (stdinSource.read(bufferedSink.buffer(), 1024) != -1) {
                            bufferedSink.flush()
                        }
                        bufferedSink.close()
                    } catch (Exception e) {
                        log.error("error", e)
                    } finally {
                        client.dispatcher().executorService().shutdown()
                    }

                    done(response)
                }
            }).start()
        }

        if (attachConfig.streams.stdout != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        def bufferedStdout = Okio.buffer(Okio.sink(attachConfig.streams.stdout))
                        while (connectionProvider.source.read(bufferedStdout.buffer(), 1024) != -1) {
                            bufferedStdout.flush()
                        }
                    } catch (Exception e) {
                        log.error("error", e)
                    }
                }
            }).start()
        }
        onResponse(response)
    }
}
