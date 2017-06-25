package de.gesellix.docker.engine

import okhttp3.WebSocket
import okhttp3.WebSocketListener

interface EngineClient {

    EngineResponse head(Map requestConfig)

    EngineResponse get(Map requestConfig)

    EngineResponse put(Map requestConfig)

    EngineResponse post(Map requestConfig)

    EngineResponse delete(Map requestConfig)

    WebSocket webSocket(Map requestConfig, WebSocketListener listener)
}
