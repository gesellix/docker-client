package de.gesellix.docker.client

import de.gesellix.docker.client.protocolhandler.contenthandler.RawInputStream
import groovy.json.JsonSlurper
import org.apache.commons.io.IOUtils
import org.joda.time.DateTime
import spock.lang.Ignore
import spock.lang.Requires
import spock.lang.Specification

import java.util.concurrent.CountDownLatch

import static java.util.concurrent.TimeUnit.SECONDS

@Requires({ LocalDocker.available() })
class DockerClientImplExplorationTest extends Specification {

    DockerClient dockerClient

    def setup() {
        dockerClient = new DockerClientImpl(
//                config: new DockerConfig(
//                        dockerHost: "http://192.168.99.100:2376",
//                        certPath: "/Users/${System.getProperty('user.name')}/.docker/machine/machines/default")
        )
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

    @Ignore("only for explorative testing")
    def "events (poll)"() {
        // meh. boot2docker/docker-machine sometimes need a time update, e.g. via:
        // docker-machine ssh default 'sudo ntpclient -s -h pool.ntp.org'

        given:
        def dockerSystemTime = DateTime.parse(dockerClient.info().content.SystemTime as String)
        long dockerEpoch = dockerSystemTime.millis / 1000

        def localSystemTime = DateTime.now()
        long localEpoch = localSystemTime.millis / 1000

        long timeOffset = localEpoch - dockerEpoch

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

        def container1 = dockerClient.createContainer([Cmd: "-"]).content.Id
        def container2 = dockerClient.createContainer([Cmd: "-"]).content.Id

        Thread.sleep(1000)
        long epochBeforeRm = (DateTime.now().millis / 1000) + timeOffset
        dockerClient.rm(container1)

        when:
        dockerClient.events(callback, [since: epochBeforeRm])
        latch.await(5, SECONDS)

        then:
        callback.events.size() == 1
        and:
        callback.events.first().status == "destroy"
        callback.events.first().id == container1

        cleanup:
        dockerClient.rm(container2)
    }

    @Ignore("only for explorative testing")
    def top() {
        when:
        def top = dockerClient.top("foo").content

        then:
        top == [Titles   : ["UID", "PID", "PPID", "C", "STIME", "TTY", "TIME", "CMD"],
                Processes: [["root", "19265", "964", "0", "21:37", "pts/0", "00:00:00", "ping 127.0.0.1"]]
        ]
    }

    @Ignore("only for explorative testing")
    def stats() {
        given:
        def latch = new CountDownLatch(1)
        def callback = new DockerAsyncCallback() {
            def stats = []

            @Override
            def onEvent(Object stat) {
                println stat
                stats << new JsonSlurper().parseText(stat as String)
                latch.countDown()
            }
        }

        when:
        dockerClient.stats("foo", callback)
        latch.await(5, SECONDS)

        then:
        callback.stats.size() == 1
        callback.stats.first().blkio_stats
    }

    @Ignore("only for explorative testing")
    def logs() {
        given:
        def latch = new CountDownLatch(1)
        def callback = new DockerAsyncCallback() {
            def lines = []

            @Override
            def onEvent(Object line) {
                println line
                lines << line
                latch.countDown()
            }
        }

        when:
        dockerClient.logs("foo", [tail: 1], callback)
        latch.await(5, SECONDS)

        then:
        callback.lines.size() == 1
        callback.lines.first().startsWith("64 bytes from 127.0.0.1")
    }
}
