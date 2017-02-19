package de.gesellix.docker.compose.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson
import de.gesellix.docker.compose.types.Command

class StringOrListToCommandAdapter {

    @ToJson
    List<String> toJson(@CommandType Command command) {
        throw new UnsupportedOperationException()
    }

    @FromJson
    @CommandType
    Command fromJson(JsonReader reader) {
        def command = new Command(parts: [])
        def token = reader.peek()
        if (token == JsonReader.Token.BEGIN_ARRAY) {
            reader.beginArray()
            while (reader.hasNext()) {
                command.parts.add(reader.nextString())
            }
            reader.endArray()
        } else if (token == JsonReader.Token.STRING) {
            command.parts.add(reader.nextString())
        } else {
            // ...
        }
        return command
    }
}
