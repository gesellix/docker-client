package de.gesellix.docker.testutil

import org.newsclub.net.unix.AFUNIXServerSocket
import org.newsclub.net.unix.AFUNIXSocketAddress
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.CountDownLatch

// from https://github.com/gesellix/junixsocket/blob/master/junixsocket/src/demo/org/newsclub/net/unix/demo/SimpleTestServer.java
class UnixSocketTestServer {

    Logger logger = LoggerFactory.getLogger(UnixSocketTestServer)

    def greeting = "welcome!"
    def sendGreeting = false
    def keepRunning = false
    def constantResponse = null

    File socketFile
    def socketThread = null

    public static void main(String[] args) {
        File socketFile = new File(new File(System.getProperty("java.io.tmpdir")), "unixsocket-server.sock")
        def server = new UnixSocketTestServer(socketFile)
        server.with {
            sendGreeting = true
            keepRunning = true
        }
        server.runInNewThread().join()
        socketFile.deleteOnExit()
    }

    UnixSocketTestServer(File socketFile) {
        this.socketFile = socketFile
        if (socketFile.exists()) {
            throw new IllegalStateException("$socketFile already exists - please remove!")
        }
    }

    def runInNewThread() throws IOException {
        def startedLatch = new CountDownLatch(1)
        socketThread = Thread.start {
//    Thread.start {
            AFUNIXServerSocket server = AFUNIXServerSocket.newInstance()
            server.bind(new AFUNIXSocketAddress(socketFile))
            logger.info("server: " + server)
            logger.info("chat with me: 'socat UNIX:${socketFile} -'")

            loop(server, startedLatch)
        }
        startedLatch.await()
    }

    def loop(AFUNIXServerSocket server, CountDownLatch startedLatch) {
        def sock
        def is
        def os
        def requiresNewConnection = true
        while (!Thread.interrupted()) {
            if (requiresNewConnection) {
                logger.info("waiting for a new connection...")
                startedLatch.countDown()
                sock = server.accept()
                logger.info("connected: " + sock)
                is = sock.getInputStream()
                os = sock.getOutputStream()
                requiresNewConnection = false

                if (sendGreeting) {
                    print("return greeting to client...")
                    os.write(greeting.bytes)
                    os.flush()
                }
                logger.info("- ok, let's chat!")
            }
            assert sock && is && os

            byte[] buf = new byte[128]
            int read
            try {
                read = is.read(buf)
            }
            catch (Exception e) {
                logger.warn("got an error reading bytes from the InputStream", e)
                return
            }
            if (read == -1) {
                logger.info("EndOfStream - closing connection...")
                requiresNewConnection = true

                closeAll(os, is, sock)
            } else if (read >= 0) {
                def clientMessage = new String(buf, 0, read)
                print("> $clientMessage")

                def response = constantResponse ?: "yo $clientMessage"
                print("< $response")
                os.write(response.bytes)
                os.flush()

                if (!keepRunning) {
                    closeAll(os, is, sock)
                }
            }
        }
    }

    def closeAll(os, is, sock) {
        os.close()
        is.close()
        sock.close()
    }

    def stop() {
        Thread moribund = socketThread;
        socketThread = null;
        moribund?.interrupt();
    }
}
