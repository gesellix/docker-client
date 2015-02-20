package de.gesellix.docker.client.protocolhandler

import de.gesellix.docker.client.DockerClientImpl
import de.gesellix.socketfactory.https.KeyStoreUtil

import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

import static de.gesellix.socketfactory.https.KeyStoreUtil.getKEY_STORE_PASSWORD
import static javax.net.ssl.TrustManagerFactory.defaultAlgorithm

// Groovy version of https://gist.github.com/m451/1dfd41460b45ea17eb71
// Proof-of-concept for https://docs.docker.com/reference/api/docker_remote_api_v1.17/#attach-to-a-container
class RawDockerStreamReader {

  private String dockerHost
  private sslContext

  public static void main(String[] args) throws IOException {
    def dockerHost = System.env.DOCKER_HOST.replace("tcp://", "https://")
    def dockerClient = new DockerClientImpl(dockerHost: dockerHost)
    dockerClient.ping()

    def testUrl = "${dockerHost}/containers/test/attach?logs=1&stream=1&stdout=1&stderr=0&tty=false"
    new RawDockerStreamReader(dockerHost).attach(testUrl)
  }

  RawDockerStreamReader(dockerHost) {
    this.dockerHost = dockerHost

    def dockerCertPath = System.getProperty("docker.cert.path", System.env.DOCKER_CERT_PATH)
    def keyStore = KeyStoreUtil.createDockerKeyStore(new File(dockerCertPath).absolutePath)
    final KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(getDefaultAlgorithm());
    kmfactory.init(keyStore, KEY_STORE_PASSWORD as char[]);
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(getDefaultAlgorithm());
    tmf.init(keyStore)
    sslContext = SSLContext.getInstance("TLS")
    sslContext.init(kmfactory.keyManagers, tmf.trustManagers, null)
  }

  def attach(testUrl) {
    // Remember that we will only see output if there is output in the specified container stream (Stdout and/or Stderr)
    URL url = new URL(testUrl)
    HttpsURLConnection con = (HttpsURLConnection) url.openConnection()
    con.setSSLSocketFactory(sslContext.socketFactory)
    con.setRequestMethod("POST")
    // since we listen to a stream we disable the timeout
    con.setConnectTimeout(0)
    // since we listen to a stream we disable the timeout
    con.setReadTimeout(0)
    con.setUseCaches(false)

    int status = con.getResponseCode()
    println "Returncode: $status"

    InputStream stream = con.getInputStream()
    def dataInput = new DataInputStream(stream)

    int max = 10;
    while (true) {
      def header = new RawDockerHeader(dataInput)
      println "stream type: ${header.streamType}"
      def frameSize = header.frameSize
      def count = 0
      while (frameSize > 0) {
        print((char) dataInput.readByte())
        count++
        frameSize--
      }

      max--
      if (count == -1 || max <= 0) {
        break
      }
    }

    dataInput.close()
    stream.close()
  }
}
