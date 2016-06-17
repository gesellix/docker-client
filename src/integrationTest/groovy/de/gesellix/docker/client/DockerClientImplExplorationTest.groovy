package de.gesellix.docker.client

import de.gesellix.docker.client.rawstream.RawInputStream
import org.apache.commons.io.IOUtils
import spock.lang.Ignore
import spock.lang.Requires
import spock.lang.Specification

@Requires({ LocalDocker.available() })
class DockerClientImplExplorationTest extends Specification {

    DockerClient dockerClient

    def setup() {
        dockerClient = new DockerClientImpl()
    }

    @Ignore("only for explorative testing")
    def "attach with container.config.tty=false"() {
        when:
        def attached = dockerClient.attach("test-d", [logs  : false,
                                                      stream: true,
                                                      stdin : false,
                                                      stdout: true,
                                                      stderr: false])

        then:
        attached.status.code == 200
        and:
        attached.stream instanceof RawInputStream
        and:
        attached.stream.multiplexStreams == true
        IOUtils.copy(attached.stream, System.out)
    }

    @Ignore("only for explorative testing")
    def "attach with container.config.tty=true"() {
        when:
        def attached = dockerClient.attach("test-it", [logs  : false,
                                                       stream: true,
                                                       stdin : false,
                                                       stdout: true,
                                                       stderr: false])

        then:
        attached.status.code == 200
        and:
        attached.stream instanceof RawInputStream
        and:
        attached.stream.multiplexStreams == false
        IOUtils.copy(attached.stream, System.out)
    }
}
