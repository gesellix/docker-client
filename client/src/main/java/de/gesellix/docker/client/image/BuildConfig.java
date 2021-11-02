package de.gesellix.docker.client.image;

import de.gesellix.docker.client.DockerAsyncCallback;
import de.gesellix.docker.client.Timeout;

import java.util.HashMap;
import java.util.Map;

import static de.gesellix.docker.client.Timeout.TEN_MINUTES;

public class BuildConfig {

  private Map<String, Object> query = new HashMap<>();
  private Map<String, Object> options = new HashMap<>();
  private DockerAsyncCallback callback;
  private Timeout timeout = TEN_MINUTES;

  public BuildConfig() {
    this.query.put("rm", true);
  }

  public Map<String, Object> getQuery() {
    return query;
  }

  public void setQuery(Map<String, Object> query) {
    this.query = query;
  }

  public Map<String, Object> getOptions() {
    return options;
  }

  public void setOptions(Map<String, Object> options) {
    this.options = options;
  }

  public DockerAsyncCallback getCallback() {
    return callback;
  }

  public void setCallback(DockerAsyncCallback callback) {
    this.callback = callback;
  }

  public Timeout getTimeout() {
    return timeout;
  }

  public void setTimeout(Timeout timeout) {
    this.timeout = timeout;
  }
}
