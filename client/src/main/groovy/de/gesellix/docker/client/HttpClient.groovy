package de.gesellix.docker.client

import okhttp3.WebSocket
import okhttp3.WebSocketListener

interface HttpClient {

    DockerResponse head(Map requestConfig)

    DockerResponse get(Map requestConfig)

    DockerResponse put(Map requestConfig)

    DockerResponse post(Map requestConfig)

    DockerResponse delete(Map requestConfig)

    WebSocket webSocket(Map requestConfig, WebSocketListener listener)
}
