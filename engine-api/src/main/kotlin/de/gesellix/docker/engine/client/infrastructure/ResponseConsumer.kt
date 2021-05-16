package de.gesellix.docker.engine.client.infrastructure

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import de.gesellix.docker.engine.api.Frame
import de.gesellix.docker.response.JsonChunksReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody
import okhttp3.internal.closeQuietly
import okio.appendingSink
import okio.buffer
import java.io.File
import java.lang.reflect.Type
import java.nio.file.Files

fun ResponseBody?.consumeFile(): File? {
  if (this == null) {
    return null
  }
  val f = Files.createTempFile("tmp.de.gesellix.docker.client", null).toFile()
  f.deleteOnExit()
  val sink = f.appendingSink().buffer()
  sink.writeAll(source())
  sink.close()
  closeQuietly()
  return f
}

inline fun <reified T : Any?> ResponseBody?.consumeStream(mediaType: String?): Flow<T> {
  if (this == null) {
    return emptyFlow()
  }
  when (mediaType) {
    "application/json" -> {
      val reader = JsonChunksReader<T>(source(), Serializer.moshi)
      val events = flow {
        while (reader.hasNext()) {
          val next = reader.readNext(T::class.java)
          emit(next)
        }
        this@consumeStream.closeQuietly()
      }
      return events
    }
    else -> {
      throw UnsupportedOperationException("Can't handle media type $mediaType")
    }
  }
}

inline fun ResponseBody?.consumeFrames(mediaType: String?, expectMultiplexedResponse: Boolean = false): Flow<Frame> {
  if (this == null) {
    return emptyFlow()
  }
  when (mediaType) {
    "application/vnd.docker.raw-stream" -> {
      val reader = FrameReader(source(), expectMultiplexedResponse)
      val events = flow {
        while (reader.hasNext()) {
          val next = reader.readNext(Frame::class.java)
          emit(next)
        }
        this@consumeFrames.closeQuietly()
      }
      return events
    }
    else -> {
      throw UnsupportedOperationException("Can't handle media type $mediaType")
    }
  }
}

inline fun <reified T : Any?> ResponseBody.consumeJson(): T? {
  val content = string()
  return Serializer.moshi.adapter(T::class.java).fromJson(content)
}

inline fun <reified T : Any?> ResponseBody.consumeJson(type: Type): T? {
  val content = string()
  val adapterType: Type = Types.newParameterizedType(T::class.java, type)
  val adapter: JsonAdapter<T?> = Serializer.moshi.adapter(adapterType)
  return adapter.fromJson(content)
}

//fun <T> Flow<T>.takeUntilSignal(signal: Flow<Unit>): Flow<T> = flow {
//  try {
//    coroutineScope {
//      launch {
//        signal.take(1).collect()
//        println("signalled")
//        this@coroutineScope.cancel()
//      }
//
//      collect {
//        emit(it)
//      }
//    }
//
//  } catch (e: CancellationException) {
//    //ignore
//  }
//}
