package de.gesellix.docker.client.image;

import de.gesellix.docker.engine.EngineResponse;

import java.util.List;

public class BuildResult {

  private String imageId;
  private List<Object> log;
  private EngineResponse response;

  public String getImageId() {
    return imageId;
  }

  public void setImageId(String imageId) {
    this.imageId = imageId;
  }

  public List<Object> getLog() {
    return log;
  }

  public void setLog(List<Object> log) {
    this.log = log;
  }

  public EngineResponse getResponse() {
    return response;
  }

  public void setResponse(EngineResponse response) {
    this.response = response;
  }
}
