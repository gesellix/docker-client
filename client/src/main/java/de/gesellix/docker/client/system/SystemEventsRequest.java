package de.gesellix.docker.client.system;

import java.util.Objects;

public class SystemEventsRequest {

  private String since;
  private String until;
  private String filters;

  public SystemEventsRequest() {
  }

  public SystemEventsRequest(String since, String until, String filters) {
    this.since = since;
    this.until = until;
    this.filters = filters;
  }

  public String getSince() {
    return since;
  }

  public void setSince(String since) {
    this.since = since;
  }

  public String getUntil() {
    return until;
  }

  public void setUntil(String until) {
    this.until = until;
  }

  public String getFilters() {
    return filters;
  }

  public void setFilters(String filters) {
    this.filters = filters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {return true;}
    if (o == null || getClass() != o.getClass()) {return false;}
    SystemEventsRequest that = (SystemEventsRequest) o;
    return Objects.equals(since, that.since) && Objects.equals(until, that.until) && Objects.equals(filters, that.filters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(since, until, filters);
  }

  @Override
  public String toString() {
    return "SystemEventsRequest{" +
           "since='" + since + '\'' +
           ", until='" + until + '\'' +
           ", filters='" + filters + '\'' +
           '}';
  }
}
