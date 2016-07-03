package de.gesellix.docker.client

import okhttp3.Response

class AttachConfig {

    Streams streams = new Streams(stdin: null, stdout: System.out, stderr: System.err)

    def onFailure = { Exception e -> }

    def onResponse = { Response response -> }

    def onStdinClosed = { Response response -> }

    static class Streams {
        InputStream stdin = null
        OutputStream stdout = System.out
        OutputStream stderr = System.err
    }
}
