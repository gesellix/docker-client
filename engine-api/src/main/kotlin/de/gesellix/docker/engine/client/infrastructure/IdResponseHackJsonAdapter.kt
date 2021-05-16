package de.gesellix.docker.engine.client.infrastructure

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.internal.Util
import kotlin.String

data class IdResponseHack(val id: String)

class IdResponseHackJsonAdapter(moshi: Moshi) : JsonAdapter<IdResponseHack>() {

  private val options: JsonReader.Options = JsonReader.Options.of("ID")

  private val stringAdapter: JsonAdapter<String> = moshi.adapter(String::class.java, emptySet(), "ID")

  override fun toString(): String = "CustomJsonAdapter(IdResponseHack)"

  override fun fromJson(reader: JsonReader): IdResponseHack {
    var id: String? = null
    reader.beginObject()
    while (reader.hasNext()) {
      when (reader.selectName(options)) {
        0 -> id = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull("ID", "ID", reader)
        -1 -> {
          // Unknown name, skip it.
          reader.skipName()
          reader.skipValue()
        }
      }
    }
    reader.endObject()
    return IdResponseHack(
      id = id ?: throw Util.missingProperty("ID", "ID", reader)
    )
  }

  override fun toJson(writer: JsonWriter, value: IdResponseHack?) {
    if (value == null) {
      throw NullPointerException("value_ was null! Wrap in .nullSafe() to write nullable values.")
    }
    writer.beginObject()
    writer.name("ID")
    stringAdapter.toJson(writer, value.id)
    writer.endObject()
  }
}
