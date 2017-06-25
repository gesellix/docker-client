package de.gesellix.docker.rawstream

/**
 <p>
 STREAM_TYPE can be:
 <ul>
 <li>0: stdin (will be written on stdout)</li>
 <li>1: stdout</li>
 <li>2: stderr</li>
 </ul>
 </p>
 */
enum StreamType {

    STDIN((byte) 0),
    STDOUT((byte) 1),
    STDERR((byte) 2)

    private final byte streamTypeId;

    StreamType(streamTypeId) {
        this.streamTypeId = streamTypeId
    }

    static valueOf(byte b) {
        def value = values().find {
            return it.streamTypeId == b
        }
        if (!value) {
            throw new IllegalArgumentException("no enum value for ${b} found.")
        }
        return value
    }

    byte getStreamTypeId() {
        return streamTypeId
    }
}
