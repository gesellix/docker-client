package de.gesellix.util

import okio.Okio
import okio.Sink
import okio.Source

class IOUtils {

    static long consumeToDevNull(InputStream source) {
        return copy(Okio.source(source), Okio.blackhole())
    }

    static long copy(InputStream source, OutputStream sink) {
        return copy(Okio.source(source), Okio.sink(sink))
    }

    static long copy(Source source, Sink sink) {
        def buffer = Okio.buffer(sink as Sink)
        def count = buffer.writeAll(source)
        buffer.flush()
        return count
    }

    static String toString(InputStream source) {
        Okio.buffer(Okio.source(source)).readUtf8()
    }

    static void closeQuietly(InputStream stream) {
        try {
            if (stream != null) {
                stream.close()
            }
        }
        catch (Exception ignored) {
        }
    }

    static void closeQuietly(Source source) {
        try {
            if (source != null) {
                source.close()
            }
        }
        catch (Exception ignored) {
        }
    }
}
