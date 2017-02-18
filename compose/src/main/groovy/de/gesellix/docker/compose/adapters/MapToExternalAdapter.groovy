package de.gesellix.docker.compose.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson
import de.gesellix.docker.compose.types.External

class MapToExternalAdapter {

    @ToJson
    Map<String, String> toJson(@ExternalType External external) {
        throw new UnsupportedOperationException()
    }

    @FromJson
    @ExternalType
    External fromJson(JsonReader reader) {
        def external = new External()
        def token = reader.peek()
        if (token == JsonReader.Token.BOOLEAN) {
            external.external = reader.nextBoolean()
        } else if (token == JsonReader.Token.BEGIN_OBJECT) {
            reader.beginObject()
            def name = reader.nextName()
            def value = reader.nextString()
            external.name = value
            reader.endObject()
        } else {
            // ...
        }
        return external
    }
}
