package de.gesellix.docker.client

import okhttp3.ws.WebSocketCall

public interface HttpClient {

    def head(String path)

    def head(Map requestConfig)

    def get(String path)

    def get(Map requestConfig)

    def put(String path)

    def put(Map requestConfig)

    def post(String path)

    def post(Map requestConfig)

    def delete(String path)

    def delete(Map requestConfig)

    WebSocketCall webSocketCall(Map requestConfig)
}
