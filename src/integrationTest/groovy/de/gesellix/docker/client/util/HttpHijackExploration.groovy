package de.gesellix.docker.client.util

import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.DockerClientImpl
import groovy.util.logging.Slf4j
import okhttp3.Response

import java.util.concurrent.CountDownLatch

@Slf4j
class HttpHijackExploration {

    private DockerClient dockerClient
    private Set runningContainers

    public static void main(String[] args) {
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

        def containerName = "ping"

//        dockerClient.rm(containerName)

        def runResult = dockerClient.run(
                "gesellix/docker-client-testimage",
                [
                        Cmd      : ["/bin/sh", "-c", "read line && echo \$line"],
                        OpenStdin: true,
                        Tty      : true
                ],
                "", containerName)

        log.info "${runResult.container.content.Id}"
        runningContainers << containerName

        def stdin = new ByteArrayInputStream(("more input!\n").getBytes())
        def doneLatch = new CountDownLatch(1)
        def done = { Response response ->
//            println response.body().string()
            doneLatch.countDown()
        }
        def responseLatch = new CountDownLatch(1)
        def onResponseCallback = { Response response ->
//            println response.body().string()
            responseLatch.countDown()
        }

        dockerClient.attach(
                containerName,
                [
                        logs  : false,
                        stream: true,
                        stdin : true,
                        stdout: true,
                        stderr: true
                ], [stdin             : stdin,
                    stdout            : System.out,
                    stderr            : System.err,
                    onResponseCallback: onResponseCallback,
                    done              : done])
        responseLatch.await()
        doneLatch.await()
    }

    static class ShutdownContainersHook extends Thread {

        private DockerClient dockerClient
        private Set runningContainers

        ShutdownContainersHook(DockerClient dockerClient, Set runningContainers) {
            this.dockerClient = dockerClient
            this.runningContainers = runningContainers
        }

        public void run() {
            for (String containerId : runningContainers) {
                log.info containerId

                dockerClient.stop(containerId)
                dockerClient.wait(containerId)
                dockerClient.rm(containerId)
            }
        }
    }
}
