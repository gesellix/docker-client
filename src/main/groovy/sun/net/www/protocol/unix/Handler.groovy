package sun.net.www.protocol.unix

import de.gesellix.docker.client.protocolhandler.urlstreamhandler.UnixSocketURLStreamHandler

/**
 * will be discovered and instantiated by the URL class.
 * @see URLStreamHandler
 */
class Handler extends UnixSocketURLStreamHandler {
}
