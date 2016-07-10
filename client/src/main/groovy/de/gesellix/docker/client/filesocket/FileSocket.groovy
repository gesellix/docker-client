package de.gesellix.docker.client.filesocket

import groovy.util.logging.Slf4j

@Slf4j
abstract class FileSocket extends Socket {

    static final String SOCKET_MARKER = ".socket"

    static String encodeHostname(String hostname) {
        return "${HostnameEncoder.encode(hostname)}${SOCKET_MARKER}"
    }

    static String decodeHostname(InetAddress address) {
        String hostName = address.getHostName()
        return HostnameEncoder.decode(hostName.substring(0, hostName.indexOf(SOCKET_MARKER)))
    }
}
