package de.gesellix.docker.engine

import okhttp3.Response

class AttachConfig {

    Streams streams = new Streams(stdin: null, stdout: System.out, stderr: System.err)

    def onFailure = { Exception e -> }

    def onResponse = { Response response -> }

    def onSinkClosed = { Response response -> }

    def onSourceConsumed = {}

    static class Streams {

        InputStream stdin = null
        OutputStream stdout = System.out
        OutputStream stderr = System.err
    }
}
