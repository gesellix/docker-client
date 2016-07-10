package de.gesellix.docker.client

import groovy.util.logging.Slf4j
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ws.WebSocket
import okhttp3.ws.WebSocketListener
import okio.Buffer

@Slf4j
class DefaultWebSocketListener implements WebSocketListener {

    @Override
    void onOpen(WebSocket webSocket, Response response) {
        log.debug "[onOpen]"
    }

    @Override
    void onFailure(IOException e, Response response) {
        log.debug "[onFailure] ${e.message}"
        e.printStackTrace()
    }

    @Override
    void onMessage(ResponseBody message) throws IOException {
        log.debug "[onMessage] ${message.string()}"
    }

    @Override
    void onPong(Buffer payload) {
        log.debug "[onPong]"
    }

    @Override
    void onClose(int code, String reason) {
        log.debug "[onClose] $code '$reason'"
    }
}
