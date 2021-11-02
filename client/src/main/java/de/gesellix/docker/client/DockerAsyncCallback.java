package de.gesellix.docker.client;

public interface DockerAsyncCallback {

  Object onEvent(Object event);

  Object onFinish();
}
