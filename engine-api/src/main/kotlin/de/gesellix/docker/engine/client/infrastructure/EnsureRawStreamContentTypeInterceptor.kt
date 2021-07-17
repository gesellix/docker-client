package de.gesellix.docker.engine.client.infrastructure

import okhttp3.Interceptor
import okhttp3.Response

data class MultiplexedStreamConfig(val expectMultiplexedStream: Boolean)

// This one would work automatically, when the response content-type would be set correctly :-/
// see https://github.com/gesellix/docker-client/issues/21
class EnsureRawStreamContentTypeInterceptor : Interceptor {

  override fun intercept(chain: Interceptor.Chain): Response {
    val response = chain.proceed(chain.request())
    if (chain.request().tag(MultiplexedStreamConfig::class.java)?.expectMultiplexedStream == true) {
      if (response.headers("Content-Type").isEmpty()) {
        // TODO use a proper logger
        println("Overriding Content-Type response header with application/vnd.docker.raw-stream")
        return response.newBuilder().header("Content-Type", "application/vnd.docker.raw-stream").build()
      }
    }
    return response
  }
}
