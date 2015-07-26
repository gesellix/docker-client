package de.gesellix.docker.client

import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_17
import org.java_websocket.handshake.ServerHandshake

import javax.net.ssl.SSLContext
import java.nio.ByteBuffer

class DockerWebsocketClient extends WebSocketClient {

  def handler

  DockerWebsocketClient(URI serverUri, handler) {
    // the 'Origin' is necessary as long as Docker server verifies the header's existence
    // see https://github.com/docker/docker/issues/14742
    super(serverUri, new Draft_17(), ["Origin": "http://localhost"], 0)
    this.handler = handler ?: new DefaultWebsocketHandler()
  }

  DockerWebsocketClient(URI serverUri, handler, SSLContext sslContext) {
    this(serverUri, handler)
//    setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sslContext))
    setSocket(sslContext.getSocketFactory().createSocket())
  }

  @Override
  void onOpen(ServerHandshake handshakedata) {
    handler.onOpen(handshakedata)
  }

//  @Override
//  void onFragment(Framedata frame) {
//    println "received fragment: ${new String(frame.getPayloadData().array())}"
//  }

  @Override
  void onMessage(String message) {
    handler.onMessage(message)
  }

  @Override
  void onMessage(ByteBuffer blob) {
    onMessage("<binary>")
  }

  @Override
  void onClose(int code, String reason, boolean remote) {
    handler.onClose(code, reason, remote)
  }

  @Override
  void onError(Exception ex) {
    handler.onError(ex)
  }
}
