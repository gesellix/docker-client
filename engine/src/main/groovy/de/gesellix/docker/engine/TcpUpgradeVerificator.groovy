package de.gesellix.docker.engine

import groovy.util.logging.Slf4j
import okhttp3.Response

@Slf4j
class TcpUpgradeVerificator {

    static ensureTcpUpgrade(Response response) {
        if (response.code() != 101) {
            log.error "expected status 101, but got ${response.code()} ${response.message()}"
            throw new ProtocolException("Expected HTTP 101 Connection Upgrade")
        }
        String headerConnection = response.header("Connection")
        if (headerConnection.toLowerCase() != "upgrade") {
            log.error "expected 'Connection: Upgrade', but got 'Connection: ${headerConnection}'"
            throw new ProtocolException("Expected 'Connection: Upgrade'")
        }
        String headerUpgrade = response.header("Upgrade")
        if (headerUpgrade.toLowerCase() != "tcp") {
            log.error "expected 'Upgrade: tcp', but got 'Upgrade: ${headerUpgrade}'"
            throw new ProtocolException("Expected 'Upgrade: tcp'")
        }
    }
}
