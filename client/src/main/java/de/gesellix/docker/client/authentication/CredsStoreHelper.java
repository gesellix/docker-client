package de.gesellix.docker.client.authentication;

import com.squareup.moshi.Moshi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CredsStoreHelper {

  private static final Logger log = LoggerFactory.getLogger(CredsStoreHelper.class);
  private final Moshi moshi = new Moshi.Builder().build();

  public CredsStoreHelperResult getAuthentication(String credsStore, String hostname) {
    Result result = execCredsHelper(credsStore, "get", hostname);
    return toCredsStoreHelperResult(result, credsStore);
  }

  public CredsStoreHelperResult getAuthentication(String credsStore) {
    return getAuthentication(credsStore, "https://index.docker.io/v1/");
  }

  public CredsStoreHelperResult getAllAuthentications(String credsStore) {
    Result result = execCredsHelper(credsStore, "list", "unused");
    return toCredsStoreHelperResult(result, credsStore);
  }

  public CredsStoreHelperResult toCredsStoreHelperResult(Result result, String credsStore) {
    if (!result.getSuccess()) {
      return new CredsStoreHelperResult(result.getMessage());
    }

    try {
      return new CredsStoreHelperResult(moshi.adapter(Map.class).fromJson(result.getMessage()));
    }
    catch (IOException exc) {
      log.error(MessageFormat.format("cannot parse docker-credential-{0} result", credsStore), exc);
      return new CredsStoreHelperResult(exc.getMessage());
    }
    catch (Exception exc) {
      log.error(MessageFormat.format("error trying to get credentials from docker-credential-{0}", credsStore), exc);
      return new CredsStoreHelperResult(exc.getMessage());
    }
  }

  private Result execCredsHelper(String credsStore, String command, String input) {
    Process process;
    try {
      process = new ProcessBuilder(MessageFormat.format("docker-credential-{0}", credsStore), command).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.PIPE).start();
    }
    catch (Exception exc) {
      log.error(MessageFormat.format("error trying to execute docker-credential-{0} {1}", credsStore, command), exc);
      return new Result(false, exc.getMessage());
    }

    BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));

    try {
      process.getOutputStream().write((input == null ? "".getBytes() : input.getBytes()));
      process.getOutputStream().flush();
      process.getOutputStream().close();

      process.waitFor(10, TimeUnit.SECONDS);
    }
    catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }

    if (process.exitValue() != 0) {
      log.error(MessageFormat.format("docker-credential-{0} {1} failed", credsStore, command));
    }

    return new Result(process.exitValue() == 0, buffer.lines().collect(Collectors.joining()));
  }

  public static class Result {

    private final boolean success;
    private final String message;

    public Result(boolean success, String message) {
      this.success = success;
      this.message = message;
    }

    public boolean getSuccess() {
      return success;
    }

    public boolean isSuccess() {
      return success;
    }

    public String getMessage() {
      return message;
    }
  }
}
