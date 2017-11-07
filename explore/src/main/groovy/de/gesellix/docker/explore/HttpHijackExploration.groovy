package de.gesellix.docker.explore

import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.DockerClientImpl
import de.gesellix.docker.engine.AttachConfig
import de.gesellix.docker.engine.OkResponseCallback
import groovy.util.logging.Slf4j
import okhttp3.Response

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Slf4j
class HttpHijackExploration {

    private DockerClient dockerClient
    private Set runningContainers

    static void main(String[] args) {
        def dockerClient = new DockerClientImpl()
        def runningContainers = [] as Set

        def shutdownContainersHook = new ShutdownContainersHook(dockerClient, runningContainers)
        Runtime.runtime.addShutdownHook(shutdownContainersHook)

        new HttpHijackExploration(dockerClient, runningContainers).run()
    }

    HttpHijackExploration(DockerClient dockerClient, Set runningContainers) {
        this.dockerClient = dockerClient
        this.runningContainers = runningContainers
    }

    def run() {
        log.info "${dockerClient.info().content}"
        log.info "${dockerClient.version().content}"

        def containerName = "hijack"

//        dockerClient.rm(containerName)

        def runResult = dockerClient.run(
                "gesellix/testimage",
                [
                        Cmd      : ["/bin/sh", "-c", "read line && echo \"->\$line\""],
                        OpenStdin: true,
                        Tty      : true
                ],
                "os-linux",
                containerName)

        log.info "${runResult.container.content.Id}"
        runningContainers << containerName

        def stdin = new ByteArrayInputStream(("more input!\n").getBytes())
        def closedLatch = new CountDownLatch(1)
        def closed = { Response response ->
            closedLatch.countDown()
        }
        def responseLatch = new CountDownLatch(1)
        def onResponseCallback = { Response response ->
//            println response.body().string()
            responseLatch.countDown()
        }

        def attachResponse = dockerClient.attach(
                containerName,
                [
                        logs  : false,
                        stream: true,
                        stdin : true,
                        stdout: true,
                        stderr: true
                ],
                new AttachConfig(
                        streams: new AttachConfig.Streams(
                                stdin: stdin,
                                stdout: System.out,
                                stderr: System.err),
                        onResponse: onResponseCallback,
                        onSinkClosed: closed))
        responseLatch.await(10, TimeUnit.SECONDS)
        closedLatch.await(10, TimeUnit.SECONDS)

        shutdownConnections(attachResponse.responseCallback)
    }

    static void shutdownConnections(OkResponseCallback responseCallback) {
        responseCallback.client.dispatcher().executorService().shutdown()
        responseCallback.client.connectionPool().evictAll()
        System.gc()
    }

    static class ShutdownContainersHook extends Thread {

        private DockerClient dockerClient
        private Set runningContainers

        ShutdownContainersHook(DockerClient dockerClient, Set runningContainers) {
            this.dockerClient = dockerClient
            this.runningContainers = runningContainers
        }

        void run() {
            for (String containerId : runningContainers) {
                log.info containerId

                dockerClient.stop(containerId)
                dockerClient.wait(containerId)
                dockerClient.rm(containerId)
            }
        }
    }
}
