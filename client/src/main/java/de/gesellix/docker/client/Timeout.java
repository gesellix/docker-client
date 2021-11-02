package de.gesellix.docker.client;

import java.util.concurrent.TimeUnit;

public class Timeout {

  private final long timeout;
  private final TimeUnit unit;

  public static final Timeout TEN_MINUTES = new Timeout(10, TimeUnit.MINUTES);

  public Timeout(long timeout, TimeUnit unit) {
    this.timeout = timeout;
    this.unit = unit;
  }

  public long getTimeout() {
    return timeout;
  }

  public TimeUnit getUnit() {
    return unit;
  }

  @Override
  public String toString() {
    return timeout + " " + unit;
  }
}
