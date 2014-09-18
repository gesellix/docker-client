package de.gesellix.docker.client

import org.newsclub.net.unix.AFUNIXServerSocket
import org.newsclub.net.unix.AFUNIXSocketAddress

// from https://github.com/gesellix/junixsocket/blob/master/junixsocket/src/demo/org/newsclub/net/unix/demo/SimpleTestServer.java
class UnixSocketTestServer {

  public static void runOnce(File socketFile, byte[] expectedResponse) throws IOException {
    AFUNIXServerSocket server = AFUNIXServerSocket.newInstance()
    server.bind(new AFUNIXSocketAddress(socketFile))
    System.out.println("server: " + server)

    while (!Thread.interrupted()) {
      System.out.println("Waiting for connection...")
      Socket sock = server.accept()
      System.out.println("Connected: " + sock)

      InputStream is = sock.getInputStream()
      OutputStream os = sock.getOutputStream()

      System.out.println("return expectedResponse to client " + os)
      os.write(expectedResponse)
      os.flush()

      byte[] buf = new byte[128]
      int read = is.read(buf)
      System.out.println("Client's response: " + new String(buf, 0, read))

      os.close()
      is.close()

      sock.close()
    }
  }
}
