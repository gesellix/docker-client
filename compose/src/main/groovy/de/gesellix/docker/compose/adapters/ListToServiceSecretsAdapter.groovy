package de.gesellix.docker.compose.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson
import de.gesellix.docker.compose.types.ServiceSecret

class ListToServiceSecretsAdapter {

    @ToJson
    List<Map<String, Object>> toJson(@ServiceSecretsType List<Map<String, ServiceSecret>> secrets) {
        throw new UnsupportedOperationException()
    }

    @FromJson
    @ServiceSecretsType
    List<Map<String, ServiceSecret>> fromJson(JsonReader reader) {
        def result = []
        def token = reader.peek()
        if (token == JsonReader.Token.BEGIN_ARRAY) {
            reader.beginArray()
            while (reader.hasNext()) {
                result.addAll(parseServiceSecretEntry(reader))
            }
            reader.endArray()
        } else {
            // ...
        }
        return result
    }

    List<Map<String, ServiceSecret>> parseServiceSecretEntry(JsonReader reader) {
        def entryToken = reader.peek()
        if (entryToken == JsonReader.Token.STRING) {
            def value = reader.nextString()
            return [[(value): null]]
        } else if (entryToken == JsonReader.Token.BEGIN_OBJECT) {
            reader.beginObject()
            def secret = new ServiceSecret()
            while (reader.hasNext()) {
                def name = reader.nextName()
                def valueType = reader.peek()
                if (valueType == JsonReader.Token.STRING) {
                    def value = reader.nextString()
                    secret[name] = value
                } else if (valueType == JsonReader.Token.NUMBER) {
                    def value = reader.nextInt()
                    secret[name] = value
                } else {
                    // ...
                }
            }
            reader.endObject()
            return [[(secret.source): secret]]
        } else {
            // ...
        }
        return []
    }
}
