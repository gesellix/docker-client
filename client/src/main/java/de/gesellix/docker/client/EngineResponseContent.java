package de.gesellix.docker.client;

import de.gesellix.docker.engine.EngineResponse;
import de.gesellix.docker.engine.EngineResponseStatus;

import java.io.InputStream;
import java.util.concurrent.Future;

public class EngineResponseContent<R> extends EngineResponse<R> {

  public EngineResponseContent(R content) {
    super();
    setContent(content);
  }

  @Override
  public EngineResponseStatus getStatus() {
    throw new UnsupportedOperationException("deprecated");
  }

  @Override
  public void setStatus(EngineResponseStatus status) {
    throw new UnsupportedOperationException("deprecated");
  }

  @Override
  public Object getHeaders() {
    throw new UnsupportedOperationException("deprecated");
  }

  @Override
  public void setHeaders(Object headers) {
    throw new UnsupportedOperationException("deprecated");
  }

  @Override
  public String getContentType() {
    throw new UnsupportedOperationException("deprecated");
  }

  @Override
  public void setContentType(String contentType) {
    throw new UnsupportedOperationException("deprecated");
  }

  @Override
  public String getMimeType() {
    throw new UnsupportedOperationException("deprecated");
  }

  @Override
  public void setMimeType(String mimeType) {
    throw new UnsupportedOperationException("deprecated");
  }

  @Override
  public String getContentLength() {
    throw new UnsupportedOperationException("deprecated");
  }

  @Override
  public void setContentLength(String contentLength) {
    throw new UnsupportedOperationException("deprecated");
  }

  @Override
  public InputStream getStream() {
    throw new UnsupportedOperationException("deprecated");
  }

  @Override
  public void setStream(InputStream stream) {
    throw new UnsupportedOperationException("deprecated");
  }

  @Override
  public Future<?> getTaskFuture() {
    throw new UnsupportedOperationException("deprecated");
  }

  @Override
  public void setTaskFuture(Future<?> taskFuture) {
    throw new UnsupportedOperationException("deprecated");
  }
}
