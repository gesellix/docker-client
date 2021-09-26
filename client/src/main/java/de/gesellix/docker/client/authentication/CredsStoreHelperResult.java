package de.gesellix.docker.client.authentication;

import java.util.Map;
import java.util.Objects;

public class CredsStoreHelperResult {

  private String error;
  private Map<String, Object> data;

  public CredsStoreHelperResult(String error) {
    this.error = error;
  }

  public CredsStoreHelperResult(Map<String, Object> data) {
    this.data = data;
  }

  public String getError() {
    return error;
  }

  public Map<String, Object> getData() {
    return data;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {return true;}
    if (o == null || getClass() != o.getClass()) {return false;}
    CredsStoreHelperResult that = (CredsStoreHelperResult) o;
    return Objects.equals(error, that.error) && Objects.equals(data, that.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(error, data);
  }
}
