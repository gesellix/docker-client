package de.gesellix.docker.client.protocolhandler.urlstreamhandler

import groovy.util.logging.Slf4j
import sun.net.www.MessageHeader
import sun.net.www.http.HttpClient
import sun.net.www.http.PosterOutputStream

// behave like the sun internal HttpClient,
// but connect via named pipe to the docker daemon.
@Slf4j
class HttpOverNamedPipeClient extends HttpClient {

    protected HttpOverNamedPipeClient(URL url) throws IOException {
        super(url, true)
    }

    @Override
    protected Socket doConnect(String host, int port) throws IOException, UnknownHostException {
        log.debug "connect via '${host}'..."
        return new NamedPipeSocket(host)
    }

    @Override
    void writeRequests(MessageHeader messageHeader, PosterOutputStream posterOutputStream) throws IOException {
        //printHeaders(messageHeader)
        super.writeRequests(messageHeader, posterOutputStream)
    }

    private void printHeaders(MessageHeader messageHeader) {
        def headers = messageHeader.getHeaders()
        headers.each { key, List<String> val ->
            val.each {
                println it == null ? key : "${key}: ${it}"
            }
        }
    }

    class NamedPipeSocket extends Socket {
        private RandomAccessFile namedPipe
        private boolean isClosed = false

        NamedPipeSocket(String filename) throws IOException {
            if (!filename) {
                throw new IllegalArgumentException("Illegal (empty) named pipe file name")
            }

            filename = URLDecoder.decode(filename, "UTF-8")
            filename = filename.replaceAll("/", "\\\\")
            this.namedPipe = new RandomAccessFile(filename, "rw")
        }

        @Override
        public boolean isClosed() {
            return this.isClosed
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new FileInputStream(this.namedPipe.getFD())
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return new FileOutputStream(this.namedPipe.getFD()) }

        @Override
        public synchronized void close() throws IOException {
            this.namedPipe.close()
            this.isClosed = true
        }
    }
}
