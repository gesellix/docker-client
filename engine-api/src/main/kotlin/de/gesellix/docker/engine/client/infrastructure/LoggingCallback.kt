package de.gesellix.docker.engine.client.infrastructure

import de.gesellix.docker.engine.api.Cancellable
import de.gesellix.docker.engine.api.StreamCallback

class LoggingCallback : StreamCallback<Any?> {

  private val log by logger()

  var job: Cancellable? = null
  override fun onStarting(cancellable: Cancellable) {
    job = cancellable
  }

  override fun onNext(event: Any?) {
    log.info("$event")
  }
}
