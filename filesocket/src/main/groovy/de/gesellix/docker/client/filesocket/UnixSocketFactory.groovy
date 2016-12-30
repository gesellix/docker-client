package de.gesellix.docker.client.filesocket

import org.newsclub.net.unix.AFUNIXSocket

class UnixSocketFactory extends FileSocketFactory {

    static boolean isSupported() {
        try {
            def isWindows = System.getProperty("os.name")?.toLowerCase()?.contains("windows")
            return !isWindows && AFUNIXSocket.isSupported()
        }
        catch (Throwable ignored) {
            return false
        }
    }

    @Override
    Socket createSocket() throws IOException {
        return new UnixSocket()
    }
}
