package de.gesellix.docker.client.protocolhandler.urlstreamhandler

import groovy.util.logging.Slf4j

/**
 * This is class marked as abstract, because it is normally
 * instantiated by the sun.net.www.protocol.npipe.Handler subclass.
 * @see sun.net.www.protocol.npipe.Handler
 * @see URLStreamHandler
 */
@Slf4j
abstract class NamedPipeURLStreamHandler extends URLStreamHandler {

    NamedPipeURLStreamHandler() {
        log.debug "initialize named pipe protocol support"
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        return new HttpOverNamedPipeURLConnection(u);
    }

    @Override
    protected URLConnection openConnection(URL u, Proxy p) throws IOException {
        // ignore the proxy
        return this.openConnection(u)
    }
}
