package de.gesellix.docker.client.util

import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.DockerClientImpl
import de.gesellix.docker.client.rawstream.RawInputStream
import groovy.util.logging.Slf4j
import spock.lang.Ignore
import spock.lang.Requires
import spock.lang.Specification

@Slf4j
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
        IOUtils.copy(attached.stream as InputStream, System.out)
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
        IOUtils.copy(attached.stream as InputStream, System.out)
    }

    @Ignore("only for explorative testing")
    def "cleanup volumes"() {
        given:
        def shouldKeepVolume = { Map volume ->
            log.warn("volume : ${volume}")
            def keep = volume.Name.replaceAll("^/", "").matches(".*data.*")
            if (keep) {
                log.warn("will keep ${volume}")
            }
            return keep
        }

        expect:
        dockerClient.cleanupVolumes shouldKeepVolume
    }
}
