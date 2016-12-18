package de.gesellix.docker.client

import okhttp3.WebSocket
import okhttp3.WebSocketListener

interface HttpClient {

    def head(Map requestConfig)

    def get(Map requestConfig)

    def put(Map requestConfig)

    def post(Map requestConfig)

    def delete(Map requestConfig)

    WebSocket webSocket(Map requestConfig, WebSocketListener listener)
}
