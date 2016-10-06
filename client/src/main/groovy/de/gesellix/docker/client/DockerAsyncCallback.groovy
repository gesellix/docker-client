package de.gesellix.docker.client

interface DockerAsyncCallback {

    def onEvent(event)

    def onFinish()
}
