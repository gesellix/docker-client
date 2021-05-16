package de.gesellix.docker.engine.api;

public interface StreamCallback<T> {

  default void onStarting(Cancellable cancellable) {
  }

  void onNext(T element);

  default void onFailed(Exception e) {
    throw new RuntimeException(e);
  }

  default void onFinished() {
  }
}
