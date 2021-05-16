package de.gesellix.docker.engine.client.infrastructure

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

// inspired by https://github.com/square/moshi/issues/843#issuecomment-584842727
class NullIfEmptyEnumAdapterFactory : JsonAdapter.Factory {

  override fun create(
    type: Type,
    annotations: MutableSet<out Annotation>,
    moshi: Moshi
  ): JsonAdapter<*>? {
//    if (!Types.getRawType(type).isAnnotationPresent(
//        DefaultIfEmpty::class.java)) {
//      return null
//    }
    if (!Types.getRawType(type).isEnum) {
      return null
    }

    val delegate = moshi.nextAdapter<Any>(this, type, annotations)

    return object : JsonAdapter<Any>() {
      override fun fromJson(reader: JsonReader): Any? {
//        @Suppress("UNCHECKED_CAST")
        val blob = reader.readJsonValue() as String?
//        val blob = reader.readJsonValue() as Map<String, Any?>
        val nullOrValue = when {
          (blob.isNullOrEmpty()) -> null
          else -> blob
        }
        return delegate.fromJsonValue(nullOrValue)
      }

      override fun toJson(writer: JsonWriter, value: Any?) {
        return delegate.toJson(writer, value)
      }
    }
  }
}
