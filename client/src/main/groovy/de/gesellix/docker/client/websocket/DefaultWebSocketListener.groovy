package de.gesellix.docker.client.websocket

import groovy.util.logging.Slf4j
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

@Slf4j
class DefaultWebSocketListener extends WebSocketListener {

    @Override
    void onOpen(WebSocket webSocket, Response response) {
        log.debug "[onOpen]"
    }

    @Override
    void onFailure(WebSocket webSocket, Throwable t, Response response) {
        log.debug "[onFailure] ${t.message}"
        t.printStackTrace()
    }

    @Override
    void onMessage(WebSocket webSocket, String text) {
        log.debug "[onMessage.text] ${text}"
    }

    @Override
    void onMessage(WebSocket webSocket, ByteString bytes) {
        log.debug "[onMessage.binary] size: ${bytes.size()}"
    }

    @Override
    void onClosing(WebSocket webSocket, int code, String reason) {
        log.debug "[onClosing] ${code}/${reason}"
    }

    @Override
    void onClosed(WebSocket webSocket, int code, String reason) {
        log.debug "[onClosed] ${code}/${reason}"
    }
}
