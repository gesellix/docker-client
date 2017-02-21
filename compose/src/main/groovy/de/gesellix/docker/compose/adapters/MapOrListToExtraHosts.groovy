package de.gesellix.docker.compose.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson
import de.gesellix.docker.compose.types.ExtraHosts

class MapOrListToExtraHosts {

    @ToJson
    Map<String, String> toJson(@ExtraHostsType ExtraHosts extraHosts) {
        throw new UnsupportedOperationException()
    }

    @FromJson
    @ExtraHostsType
    ExtraHosts fromJson(JsonReader reader) {
        def extraHosts = new ExtraHosts()
        def token = reader.peek()
        if (token == JsonReader.Token.BEGIN_ARRAY) {
            reader.beginArray()
            while (reader.peek() != JsonReader.Token.END_ARRAY) {
                def entry = reader.nextString()
                def keyAndValue = entry.split(":", 2)
                extraHosts.entries[keyAndValue[0]] = keyAndValue[1] ?: ""
            }
            reader.endArray()
        } else if (token == JsonReader.Token.BEGIN_OBJECT) {
            reader.beginObject()
            while (reader.peek() != JsonReader.Token.END_OBJECT) {
                def name = reader.nextName()
                String value = reader.peek() == JsonReader.Token.NULL ? reader.nextNull() : reader.nextString()
                extraHosts.entries[name] = value ?: ""
            }
            reader.endObject()
        } else {
            // ...
        }
        return extraHosts
    }
}
