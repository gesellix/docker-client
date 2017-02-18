package de.gesellix.docker.compose.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson
import de.gesellix.docker.compose.types.Network

class StringToNetworkAdapter {

    @ToJson
    List<String> toJson(@NetworksType Map<String, Network> networks) {
        throw new UnsupportedOperationException()
    }

    @FromJson
    @NetworksType
    Map<String, Network> fromJson(JsonReader reader) {
        def result = [:]
        def token = reader.peek()
        if (token == JsonReader.Token.BEGIN_OBJECT) {
            reader.beginObject()
            def name = reader.nextName()
            def value = reader.peek() == JsonReader.Token.NULL ? reader.nextNull() : reader.nextString()
            result[(name)] = value
            reader.endObject()
        } else if (token == JsonReader.Token.BEGIN_ARRAY) {
            reader.beginArray()
            def name = reader.nextString()
//            def value = reader.nextNull()
            result[(name)] = null
            reader.endArray()
        } else {
            // ...
        }
        return result
    }
}
