package de.gesellix.docker.compose.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson
import de.gesellix.docker.compose.types.Environment

class MapOrListToEnvironmentAdapter {

    @ToJson
    List<String> toJson(@EnvironmentType Environment environment) {
        throw new UnsupportedOperationException()
    }

    @FromJson
    @EnvironmentType
    Environment fromJson(JsonReader reader) {
        def environment = new Environment()
        def token = reader.peek()
        if (token == JsonReader.Token.BEGIN_ARRAY) {
            reader.beginArray()
            while (reader.peek() != JsonReader.Token.END_ARRAY) {
                def entry = reader.nextString()
                def keyAndValue = entry.split("=", 2)
                environment.entries[keyAndValue[0]] = keyAndValue[1] ?: ""
            }
            reader.endArray()
        } else if (token == JsonReader.Token.BEGIN_OBJECT) {
            reader.beginObject()
            while (reader.peek() != JsonReader.Token.END_OBJECT) {
                def name = reader.nextName()
                String value = reader.peek() == JsonReader.Token.NULL ? reader.nextNull() : reader.nextString()
                environment.entries[name] = value ?: ""
            }
            reader.endObject()
        } else {
            // ...
        }
        return environment
    }
}
