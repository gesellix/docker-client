package de.gesellix.docker.client

import groovy.util.logging.Slf4j
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.io.RealConnection
import okio.Sink
import okio.Source

@Slf4j
class ConnectionProvider implements Interceptor {

    Sink sink = null
    Source source = null

    @Override
    Response intercept(Interceptor.Chain chain) throws IOException {
        // attention: this connection is *per request*, so sink and source might be overwritten
        RealConnection connection = chain.connection() as RealConnection
        if (source != null) {
            log.warn("overwriting source")
        }
        source = connection.source
        if (sink != null) {
            log.warn("overwriting sink")
        }
        sink = connection.sink

        def response = chain.proceed(chain.request())
        return response
    }
}
