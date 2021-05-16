package de.gesellix.docker.engine.client.infrastructure

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import de.gesellix.docker.engine.model.IdResponse
import java.lang.reflect.Type

class IdResponseAdapter : JsonAdapter.Factory {

  override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi): JsonAdapter<*>? {
    if (Types.getRawType(type) != IdResponse::class.java) {
      return null
    }

    val idAdapter = IdResponseHackJsonAdapter(moshi)
    val delegate = moshi.nextAdapter<Any>(this, type, annotations)

    return object : JsonAdapter<Any>() {
      override fun fromJson(reader: JsonReader): Any? {
//        @Suppress("UNCHECKED_CAST")
        val peekJson = reader.peekJson()
        return try {
          delegate.fromJson(reader)
        } catch (e: JsonDataException) {
          val idResponseHack = idAdapter.fromJson(peekJson)
          IdResponse(id = idResponseHack.id)
        }
      }

      override fun toJson(writer: JsonWriter, value: Any?) {
        return delegate.toJson(writer, value)
      }
    }
  }
}
