package de.gesellix.docker.client

import okhttp3.ws.WebSocketCall

public interface HttpClient {

    def head(Map requestConfig)

    def get(Map requestConfig)

    def put(Map requestConfig)

    def post(Map requestConfig)

    def delete(Map requestConfig)

    WebSocketCall webSocketCall(Map requestConfig)
}
