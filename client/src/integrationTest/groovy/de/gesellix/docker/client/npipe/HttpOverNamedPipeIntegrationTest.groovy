package de.gesellix.docker.client.npipe

import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.DockerClientImpl
import de.gesellix.docker.client.HttpClient
import de.gesellix.docker.client.OkDockerClient
import de.gesellix.docker.client.util.LocalDocker
import org.apache.commons.lang.SystemUtils
import org.spockframework.util.Assert
import spock.lang.Requires
import spock.lang.Specification

import java.util.concurrent.CountDownLatch

@Requires({ SystemUtils.IS_OS_WINDOWS && LocalDocker.available() })
class HttpOverNamedPipeIntegrationTest extends Specification {

    def "http over named pipe"() {
        given:
        DockerClient docker = new DockerClientImpl()
        def npipeExe = new File("npipe.exe")
        updateNpipeExe(docker, npipeExe)

        def pipePath = "//./pipe/echo_pipe"
        def npipeLatch = new CountDownLatch(1)
        def process = runNpipe(npipeExe, pipePath, npipeLatch)

        npipeLatch.await()
        if (!process.alive) {
            Assert.fail("couldn't create a named pipe [${process.exitValue()}]")
        }

        HttpClient httpClient = new OkDockerClient("npipe://${pipePath}")

        when:
        def response = httpClient.post([path              : "/foo",
                                        requestContentType: "text/plain",
                                        body              : new ByteArrayInputStream("hello world".bytes)])

        then:
        response.status.code == 200
        response.content == "[echo] hello world"

        cleanup:
        actSilently { httpClient?.post([path: "/exit"]) }
        actSilently { process.waitFor() }
        actSilently { docker.rm("npipe") }
    }

    def actSilently(Closure action) {
        try {
            action()
        } catch (Exception ignored) {
        }
    }

    def runNpipe(File npipeExe, String pipePath, CountDownLatch npipeLatch) {
        def process = "cmd /c \"${npipeExe.absolutePath} ${pipePath}\"".execute()
        Thread.start {
            process.in.eachLine { String line ->
                println(line)
                if (line.contains(pipePath)) {
                    npipeLatch.countDown()
                }
            }
        }
        Thread.start {
            process.err.eachLine { String line ->
                println(line)
                if (line.contains(pipePath)) {
                    npipeLatch.countDown()
                }
            }
        }
        return process
    }

    def updateNpipeExe(DockerClientImpl docker, File npipeExe) {
        docker.createContainer([Image: "gesellix/npipe"], [name: "npipe"])
        def archive = docker.getArchive("npipe", "/npipe.exe").stream as InputStream
        docker.copySingleTarEntry(archive, "/npipe.exe", new FileOutputStream(npipeExe))
    }
}
