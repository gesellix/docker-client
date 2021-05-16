package de.gesellix.docker.engine.client.infrastructure

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.Source

// See https://github.com/square/okhttp/pull/3912 for a possible implementation
// Would we have issues with non-closed InputStreams?
// More details/examples:
// - https://github.com/minio/minio-java/issues/924
// - https://github.com/square/okhttp/issues/2424
//      mediaType == OctetStreamMediaType && content is InputStream -> RequestBody.create(content.source().buffer(), mediaType.toMediaTypeOrNull())
fun Source.asRequestBody(contentType: MediaType? = null): RequestBody {
  return object : RequestBody() {
    override fun contentType() = contentType

    override fun contentLength() = -1L

    override fun isOneShot(): Boolean {
      // we shouldn't allow OkHttp to retry this request
      return true
    }

    override fun writeTo(sink: BufferedSink) {
      use { source -> sink.writeAll(source) }
    }
  }
}
