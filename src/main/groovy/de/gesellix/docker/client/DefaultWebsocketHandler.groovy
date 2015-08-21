package de.gesellix.docker.client

import org.java_websocket.handshake.ServerHandshake
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DefaultWebsocketHandler {

    final Logger log = LoggerFactory.getLogger(DefaultWebsocketHandler)

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
