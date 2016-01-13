package de.gesellix.docker.client

import groovy.util.logging.Slf4j
import org.java_websocket.handshake.ServerHandshake

@Slf4j
class DefaultWebsocketHandler {

    void onOpen(ServerHandshake handshakedata) {
        log.debug "[onOpen]"
    }

    void onMessage(String message) {
        log.debug "[onMessage] $message"
    }

    void onClose(int code, String reason, boolean remote) {
        log.debug "[onClose] $code '$reason' ($remote)"
    }

    void onError(Exception ex) {
        log.debug "[onError] ${ex.message}"
        ex.printStackTrace()
    }
}
