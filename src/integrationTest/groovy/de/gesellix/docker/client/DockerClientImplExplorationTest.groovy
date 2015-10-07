package de.gesellix.docker.client

import de.gesellix.docker.client.protocolhandler.contenthandler.RawInputStream
import groovy.json.JsonSlurper
import org.apache.commons.io.IOUtils
import spock.lang.Ignore
import spock.lang.Specification

import java.util.concurrent.CountDownLatch

import static java.util.concurrent.TimeUnit.SECONDS

class DockerClientImplExplorationTest extends Specification {

    DockerClient dockerClient

    def setup() {
        def defaultDockerHost = System.env.DOCKER_HOST
//        defaultDockerHost = "unix:///var/run/docker.sock"
        defaultDockerHost = "http://192.168.99.100:2376"
        System.setProperty("docker.cert.path", "/Users/${System.getProperty('user.name')}/.docker/machine/machines/default")
//        System.setProperty("docker.cert.path", "C:\\Users\\${System.getProperty('user.name')}\\.boot2docker\\certs\\boot2docker-vm")
        dockerClient = new DockerClientImpl(dockerHost: defaultDockerHost ?: "https://192.168.59.103:2376")
    }

    @Ignore("only for explorative testing")
    def info() {
        when:
        def info = dockerClient.info()

        then:
        info == [
                Containers        : 0,
                Debug             : 1,
                Driver            : "aufs",
                DriverStatus      : [
                        ["Root Dir", "/mnt/sda1/var/lib/docker/aufs"],
                        ["Dirs", "75"]],
                ExecutionDriver   : "native-0.2",
                Images            : 75,
                IndexServerAddress: "https://index.docker.io/v1/",
                InitPath          : "/usr/local/bin/docker",
                InitSha1          : "",
                IPv4Forwarding    : 1,
                NEventsListener   : 0,
                NFd               : 10,
                NGoroutines       : 11,
                KernelVersion     : "3.16.4-tinycore64",
                MemoryLimit       : 1,
                OperatingSystem   : "Boot2Docker 1.3.0 (TCL 5.4); master : a083df4 - Thu Oct 16 17:05:03 UTC 2014",
                SwapLimit         : 1]
    }

    @Ignore("only for explorative testing")
    def version() {
        when:
        def version = dockerClient.version()

        then:
        version == [
                ApiVersion   : "1.15",
                Arch         : "amd64",
                GitCommit    : "c78088f",
                GoVersion    : "go1.3.3",
                KernelVersion: "3.16.4-tinycore64",
                Os           : "linux",
                Version      : "1.3.0"]
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

    @Ignore("only for explorative testing")
    def "events (async)"() {
        given:
        def latch = new CountDownLatch(1)
        def callback = new DockerAsyncCallback() {
            def events = []

            @Override
            def onEvent(Object event) {
                println event
                events << new JsonSlurper().parseText(event as String)
                latch.countDown()
            }
        }
        dockerClient.events(callback)

        when:
        def response = dockerClient.createContainer([Cmd: "-"])
        latch.await(5, SECONDS)

        then:
        callback.events.size() == 1
        and:
        callback.events.first().status == "create"
        callback.events.first().id == response.content.Id

        cleanup:
        dockerClient.rm(response.content.Id)
    }
}
