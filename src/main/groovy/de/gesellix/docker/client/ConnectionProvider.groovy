package de.gesellix.docker.client

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.io.RealConnection
import okio.Sink
import okio.Source

class ConnectionProvider implements Interceptor {

    Sink sink = null
    Source source = null

    @Override
    Response intercept(Interceptor.Chain chain) throws IOException {
        // attention: this connection is *per request*, so sink and source might be overwritten
        RealConnection connection = chain.connection() as RealConnection
        source = connection.source
        sink = connection.sink

        def response = chain.proceed(chain.request())
        return response
    }
}
