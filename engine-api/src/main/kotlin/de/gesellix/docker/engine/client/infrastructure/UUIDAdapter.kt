package de.gesellix.docker.engine.client.infrastructure

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.util.*

class UUIDAdapter {

  @ToJson
  fun toJson(uuid: UUID) = uuid.toString()

  @FromJson
  fun fromJson(s: String) = UUID.fromString(s)
}
