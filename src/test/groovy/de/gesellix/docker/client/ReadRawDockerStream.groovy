package de.gesellix.docker.client

// Groovy version of https://gist.github.com/m451/1dfd41460b45ea17eb71
class ReadRawDockerStream {

  // The container name or id that we want to attach to
  private final static String container = "7040cd8208703a5837d2c77a1a2f7cc7fbcade47a6cb704e1d35e310497e96aa"

  private final static Boolean showStdOut = true
  private final static Boolean showStdErr = false

  public static void main(String[] args) throws IOException {

    def dockerHost = new URI(System.env.DOCKER_HOST.replace("^tcp://", "http://"))

    // Remember that we will only see output if there is output in the specified container stream (Stdout and/or Stderr)
    URL url = new URL("http://${dockerHost.host}:${dockerHost.port}/containers/${container}/attach?logs=1&stream=1&stdout=${showStdOut}&stderr=${showStdErr}&tty=false")
    HttpURLConnection con = (HttpURLConnection) url.openConnection()

    // we have to use post as specified in API v 1.12
    con.setRequestMethod("POST")
    // since we listen to a stream we want to timeout
    con.setConnectTimeout(0)
    // since we listen to a stream we want to timeout
    con.setReadTimeout(0)
    con.setUseCaches(false)

    // Quick error checking. See Docker API and the Java HttpURLConnection documentationfor the meaning of the return codes.
    int status = con.getResponseCode()
    System.out.println("Returncode: " + status)

    InputStream stream = null
    stream = con.getInputStream()

    int b
    while (true) {
      b = stream.read()
      // We use simple int to char conversion (quick and dirty)
      // Yet it is not perfect, but good enough to get the idea of how to fetch the hijacked HTTP stream of a Docker containers StdOut and StdErr
//      System.out.print((char) b)
      System.out.print((int) b)
      if (b == -1) {
        break
      }
    }

    stream.close()
  }
}
