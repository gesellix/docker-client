package de.gesellix.docker.compose.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson
import de.gesellix.docker.compose.types.ServiceNetwork

class StringToServiceNetworksAdapter {

    @ToJson
    List<String> toJson(@ServiceNetworksType Map<String, ServiceNetwork> networks) {
        throw new UnsupportedOperationException()
    }

    @FromJson
    @ServiceNetworksType
    Map<String, ServiceNetwork> fromJson(JsonReader reader) {
        def result = [:]
        def token = reader.peek()
        if (token == JsonReader.Token.BEGIN_OBJECT) {
            reader.beginObject()
            while (reader.hasNext()) {
                def name = reader.nextName()
                def valueType = reader.peek()
                if (valueType == JsonReader.Token.NULL) {
                    result[name] = reader.nextNull()
                } else if (valueType == JsonReader.Token.STRING) {
                    result[name] = reader.nextString()
                } else if (valueType == JsonReader.Token.BEGIN_OBJECT) {
                    def serviceNetwork = new ServiceNetwork()
                    reader.beginObject()
                    while (reader.hasNext()) {
                        def attr = reader.nextName()
                        if (attr == "ipv4_address") {
                            serviceNetwork.ipv4Address = reader.nextString()
                        } else if (attr == "ipv6_address") {
                            serviceNetwork.ipv6Address = reader.nextString()
                        } else if (attr == "aliases") {
                            def aliases = []
                            reader.beginArray()
                            while (reader.hasNext()) {
                                aliases.add(reader.nextString())
                            }
                            reader.endArray()
                            serviceNetwork.aliases = aliases
                        } else {
                            // ...
                        }
                    }
                    reader.endObject()
                    result[name] = serviceNetwork
                } else {
                    // ...
                }
            }
            reader.endObject()
        } else if (token == JsonReader.Token.BEGIN_ARRAY) {
            reader.beginArray()
            def name = reader.nextString()
//            def value = reader.nextNull()
            result[name] = null
            reader.endArray()
        } else {
            // ...
        }
        return result
    }
}
