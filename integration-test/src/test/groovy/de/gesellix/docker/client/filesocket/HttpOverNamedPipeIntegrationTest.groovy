package de.gesellix.docker.client.filesocket

import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.DockerClientImpl
import de.gesellix.docker.client.HttpClient
import de.gesellix.docker.client.OkDockerClient
import de.gesellix.docker.client.LocalDocker
import org.apache.commons.lang.SystemUtils
import spock.lang.Requires
import spock.lang.Specification

import java.util.concurrent.CountDownLatch

import static java.util.concurrent.TimeUnit.SECONDS
import static org.spockframework.util.Assert.fail

@Requires({ SystemUtils.IS_OS_WINDOWS && LocalDocker.available() })
class HttpOverNamedPipeIntegrationTest extends Specification {

    def "http over named pipe"() {
        given:
        DockerClient docker = new DockerClientImpl()
        def npipeExe = createNpipeExe(docker)

        def pipePath = "//./pipe/echo_pipe"

        def npipeLatch = new CountDownLatch(1)
        def process = runNpipe(npipeExe, pipePath, npipeLatch)
        npipeLatch.await(5, SECONDS)

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
        actSilently { process.waitFor(5, SECONDS) }
        actSilently { docker.rm("npipe") }
    }

    def actSilently(Closure action) {
        try {
            action()
        } catch (Exception ignored) {
        }
    }

    def runNpipe(File npipeExe, String pipePath, CountDownLatch npipeLatch) {
        def logProcessStartup = { String line ->
            println(line)
            if (line.contains(pipePath)) {
                npipeLatch.countDown()
            }
        }

        def process = "cmd /c \"${npipeExe.absolutePath} ${pipePath}\"".execute()
        Thread.start { process.in.eachLine logProcessStartup }
        Thread.start { process.err.eachLine logProcessStartup }
        if (!process.alive) {
            fail("couldn't create a named pipe [${process.exitValue()}]")
        }
        return process
    }

    def createNpipeExe(DockerClientImpl docker) {
        def repository = LocalDocker.isNativeWindows() ? "gesellix/npipe:windows" : "gesellix/npipe"
        docker.createContainer([Image: repository], [name: "npipe"])
        def archive = docker.getArchive("npipe", "/npipe.exe").stream as InputStream

        def npipeExe = new File("npipe.exe")
        docker.copySingleTarEntry(archive, "/npipe.exe", new FileOutputStream(npipeExe))
        return npipeExe
    }
}
