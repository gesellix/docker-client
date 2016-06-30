package de.gesellix.docker.client

class AttachConfig {

    Streams streams = new Streams(stdin: null, stdout: System.out, stderr: System.err)

    def onResponse = {}
    def onFailure = {}
    def onStdinClosed = {}

    static class Streams {
        InputStream stdin = null
        OutputStream stdout = System.out
        OutputStream stderr = System.err
    }
}
