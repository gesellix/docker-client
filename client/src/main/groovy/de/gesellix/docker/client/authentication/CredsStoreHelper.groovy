package de.gesellix.docker.client.authentication

import com.squareup.moshi.Moshi
import groovy.util.logging.Slf4j

import java.util.concurrent.TimeUnit

@Slf4j
class CredsStoreHelper {

  private Moshi moshi = new Moshi.Builder().build()

  CredsStoreHelperResult getAuthentication(String credsStore, String hostname = "https://index.docker.io/v1/") {
    def result = execCredsHelper(credsStore, "get", hostname)
    return toCredsStoreHelperResult(result, credsStore)
  }

  CredsStoreHelperResult getAllAuthentications(String credsStore) {
    def result = execCredsHelper(credsStore, "list", "unused")
    return toCredsStoreHelperResult(result, credsStore)
  }

  CredsStoreHelperResult toCredsStoreHelperResult(Result result, String credsStore) {
    if (!result.success) {
      return new CredsStoreHelperResult(error: result.message)
    }

    try {
      return new CredsStoreHelperResult(data: moshi.adapter(Map).fromJson(result.message))
    }
    catch (IOException exc) {
      log.error("cannot parse docker-credential-${credsStore} result", exc)
      return new CredsStoreHelperResult(error: exc.message)
    }
    catch (Exception exc) {
      log.error("error trying to get credentials from docker-credential-${credsStore}", exc)
      return new CredsStoreHelperResult(error: exc.message)
    }
  }

  private Result execCredsHelper(String credsStore, String command, String input) {
    def process
    try {
      process = new ProcessBuilder("docker-credential-${credsStore}", command)
          .redirectErrorStream(true)
          .redirectOutput(ProcessBuilder.Redirect.PIPE)
          .start()
    }
    catch (Exception exc) {
      log.error("error trying to execute docker-credential-${credsStore} ${command}", exc)
      return new Result(
          success: false,
          message: exc.message
      )
    }

    def buffer = new BufferedReader(new InputStreamReader(process.inputStream))

    process.outputStream.write(input?.bytes)
    process.outputStream.flush()
    process.outputStream.close()

    process.waitFor(10, TimeUnit.SECONDS)

    if (process.exitValue() != 0) {
      log.error("docker-credential-${credsStore} ${command} failed")
    }
    return new Result(
        success: process.exitValue() == 0,
        message: buffer.readLines().join('')
    )
  }

  static class Result {

    boolean success
    String message
  }
}
