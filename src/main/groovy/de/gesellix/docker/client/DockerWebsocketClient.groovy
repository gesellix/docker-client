package de.gesellix.docker.client

import de.gesellix.docker.client.protocolhandler.DockerURLHandler
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_17
import org.java_websocket.handshake.ServerHandshake

import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import java.nio.ByteBuffer

import static de.gesellix.docker.client.KeyStoreUtil.getKEY_STORE_PASSWORD
import static javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm

class DockerWebsocketClient extends WebSocketClient {

//  static {
//    WebSocketImpl.DEBUG = true
//  }

  DockerWebsocketClient(URI serverUri) {
    // the 'Origin' is necessary as long as Docker server verifies the header's existence
    // see https://github.com/docker/docker/issues/14742
    super(serverUri, new Draft_17(), ["Origin": "http://localhost"], 0)
//    setSocket(createSslContext().getSocketFactory().createSocket())
  }

  // TODO this is a copy from LowLevelDockerClient - make me DRY
  def createSslContext() {
    String dockerCertPath = new DockerURLHandler().dockerCertPath
    def keyStore = KeyStoreUtil.createDockerKeyStore(new File(dockerCertPath).absolutePath)
    final KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(getDefaultAlgorithm())
    kmfactory.init(keyStore, KEY_STORE_PASSWORD as char[])
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(getDefaultAlgorithm())
    tmf.init(keyStore)
    def sslContext = SSLContext.getInstance("TLS")
    sslContext.init(kmfactory.keyManagers, tmf.trustManagers, null)
    return sslContext
  }

  @Override
  void onOpen(ServerHandshake handshakedata) {
    println "[${new Date()}] -- onOpen"
  }

  @Override
  void onMessage(String message) {
    println "[${new Date()}] -- onMessage '$message'"
//    send(message)
  }

  @Override
  void onMessage(ByteBuffer blob) {
    onMessage("<blob>")
  }

  @Override
  void onClose(int code, String reason, boolean remote) {
    println "[${new Date()}] -- onClose $code '$reason' ($remote)"
  }

  @Override
  void onError(Exception ex) {
    println "[${new Date()}] -- onError: ${ex.message}"
    ex.printStackTrace()
  }

//  @Override
//  public void onWebsocketMessageFragment(WebSocket conn, Framedata frame) {
//    FrameBuilder builder = (FrameBuilder) frame
//    builder.setTransferemasked(true)
//    getConnection().sendFrame(frame)
//  }
}
