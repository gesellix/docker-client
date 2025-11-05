package de.gesellix.docker.client;

public class EngineResponseContent<R> {

  private R content;

  public EngineResponseContent(R content) {
    this.content = content;
  }

  public R getContent() {
    return content;
  }

  public void setContent(R content) {
    this.content = content;
  }

  @Override
  public String toString() {
    return "EngineResponse{" +
        ", content=" + content +
        '}';
  }
}
