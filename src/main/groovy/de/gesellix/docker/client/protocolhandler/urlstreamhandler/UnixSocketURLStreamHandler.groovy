package de.gesellix.docker.client.protocolhandler.urlstreamhandler

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This is class marked as abstract, because it is normally
 * instantiated by the sun.net.www.protocol.unix.Handler subclass.
 * @see sun.net.www.protocol.unix.Handler
 * @see URLStreamHandler
 */
abstract class UnixSocketURLStreamHandler extends URLStreamHandler {

    Logger logger = LoggerFactory.getLogger(UnixSocketURLStreamHandler)

    UnixSocketURLStreamHandler() {
        logger.debug "initialize unix protocol support"
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        return new HttpOverUnixSocketURLConnection(u)
    }

    @Override
    protected URLConnection openConnection(URL u, Proxy p) throws IOException {
        // ignore the proxy
        return this.openConnection(u)
    }
}
