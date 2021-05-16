package de.gesellix.docker.engine.client.infrastructure

import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun <R : Any> R.logger(): Lazy<Logger> {
  return logger(this.javaClass.name)
}

fun logger(name: String): Lazy<Logger> {
  return lazy { LoggerFactory.getLogger(name) }
}
