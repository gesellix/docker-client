package de.gesellix.docker.client

interface DockerAsyncCallback {

  def onEvent(def event)

  def onFinish()
}
