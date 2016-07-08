package de.gesellix.docker.client

import okio.ByteString

class HostnameEncoder {

    /**
     * @see java.net.IDN
     */
    static final int MAX_LABEL_LENGTH = 63
    static final int MAX_HOSTNAME_LENGTH = (MAX_LABEL_LENGTH * 4)

    static String encode(String toEncode) {
        def encoded = ByteString.encodeUtf8(toEncode).hex()
        if (encoded.length() > MAX_LABEL_LENGTH && encoded.length() < MAX_HOSTNAME_LENGTH) {
            int labelCount = Math.ceil(encoded.length() / MAX_LABEL_LENGTH)
            def labels = (0..(labelCount - 1)).collect {
                def from = it * MAX_LABEL_LENGTH
                def to = from + MAX_LABEL_LENGTH
                encoded.substring(from, Math.min(to, encoded.length()))
            }
            encoded = labels.join(".")
        }
        return encoded
    }

    static String decode(String toDecode) {
        if (toDecode.contains(".")) {
            toDecode = toDecode.split("\\.").join("")
        }
        return ByteString.decodeHex(toDecode).utf8()
    }
}
