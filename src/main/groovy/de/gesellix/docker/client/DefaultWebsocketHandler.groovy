package de.gesellix.docker.client

import org.java_websocket.handshake.ServerHandshake

class DefaultWebsocketHandler {

  void onOpen(ServerHandshake handshakedata) {
    println "[${new Date()}] -- onOpen"
  }

  void onMessage(String message) {
    println "[${new Date()}] -- onMessage '$message'"
//    send(message)
  }

  void onClose(int code, String reason, boolean remote) {
    println "[${new Date()}] -- onClose $code '$reason' ($remote)"
  }

  void onError(Exception ex) {
    println "[${new Date()}] -- onError: ${ex.message}"
    ex.printStackTrace()
  }
}
