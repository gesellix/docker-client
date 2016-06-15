package de.gesellix.docker.client.protocolhandler

import de.gesellix.docker.client.DockerConfig
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Paths

class DockerURLHandlerTest extends Specification {

    @Unroll
    def "should assume TLS when tlsVerify==#tlsVerify"() {
        def certsDir = Files.createTempDirectory("certs")
        def dockerUrlHandler = new DockerURLHandler(
                config: new DockerConfig(
                        tlsVerify: tlsVerify,
                        certPath: certsDir.toString()))
        when:
        def assumeTls = dockerUrlHandler.shouldUseTls(new URL("https://example.com:2376"))
        then:
        assumeTls
        where:
        // yes, even the falsy values (0, false, no) actually enable tls-verify
        tlsVerify << ["1", "true", "yes", "0", "false", "no"]
    }

    @Unroll
    def "should not assume TLS when tlsVerify==#tlsVerify"() {
        def certsDir = Files.createTempDirectory("certs")
        def dockerUrlHandler = new DockerURLHandler(
                config: new DockerConfig(
                        tlsVerify: tlsVerify,
                        certPath: certsDir.toString()))
        when:
        def assumeTls = dockerUrlHandler.shouldUseTls(new URL("https://example.com:2376"))
        then:
        !assumeTls
        where:
        tlsVerify << [""]
    }

    def "should not assume TLS when port !== 2376"() {
        def dockerUrlHandler = new DockerURLHandler(
                config: new DockerConfig(
                        tlsVerify: null,
                        certPath: "/some/non-existing/path"))
        when:
        def assumeTls = dockerUrlHandler.shouldUseTls(new URL("https://example.com:2375"))
        then:
        !assumeTls
    }

    def "should fail when tlsVerify=1, but certs directory doesn't exist"() {
        def dockerUrlHandler = new DockerURLHandler(
                config: new DockerConfig(
                        tlsVerify: "1",
                        certPath: "/some/non-existing/path",
                        defaultCertPath: new File("/some/non-existing/default/path")))
        when:
        dockerUrlHandler.shouldUseTls(new URL("https://example.com:2375"))
        then:
        thrown(IllegalStateException)
    }

    def "should try to use the default .docker cert path"() {
        def dockerUrlHandler = new DockerURLHandler(
                config: new DockerConfig(
                        tlsVerify: null,
                        certPath: "/some/non-existing/path"))
        def defaultDockerCertPathExisted = Files.exists(Paths.get(dockerUrlHandler.config.defaultCertPath))
        if (!defaultDockerCertPathExisted) {
            Files.createDirectory(Paths.get(dockerUrlHandler.config.defaultCertPath))
        }
        when:
        def assumeTls = dockerUrlHandler.shouldUseTls(new URL("https://example.com:2376"))
        then:
        assumeTls
        cleanup:
        defaultDockerCertPathExisted || Files.delete(Paths.get(dockerUrlHandler.config.defaultCertPath))
    }

    def "should choose http for 'tcp://127.0.0.1:2375'"() {
        def dockerUrlHandler = new DockerURLHandler(
                config: new DockerConfig(
                        tlsVerify: null))
        when:
        def finalDockerHost = dockerUrlHandler.getBaseURLWithActualProtocol("tcp://127.0.0.1:2375")
        then:
        finalDockerHost == [protocol: "http", host: "127.0.0.1", port: 2375]
    }

    def "should choose http for 'http://127.0.0.1:2375'"() {
        def dockerUrlHandler = new DockerURLHandler(
                config: new DockerConfig(
                        tlsVerify: null))
        when:
        def finalDockerHost = dockerUrlHandler.getBaseURLWithActualProtocol("http://127.0.0.1:2375")
        then:
        finalDockerHost == [protocol: "http", host: "127.0.0.1", port: 2375]
    }

    def "should choose http for 'https://127.0.0.1:2376' and disabled tls"() {
        def dockerUrlHandler = new DockerURLHandler(
                config: new DockerConfig(
                        tlsVerify: ""))
        when:
        def finalDockerHost = dockerUrlHandler.getBaseURLWithActualProtocol("https://127.0.0.1:2376")
        then:
        finalDockerHost == [protocol: "http", host: "127.0.0.1", port: 2376]
    }

    def "should choose https for 'https://127.0.0.1:2376' and enabled tls"() {
        def certsDir = Files.createTempDirectory("certs")
        def dockerUrlHandler = new DockerURLHandler(
                config: new DockerConfig(
                        tlsVerify: "1",
                        certPath: certsDir.toString()))
        when:
        def finalDockerHost = dockerUrlHandler.getBaseURLWithActualProtocol("https://127.0.0.1:2376")
        then:
        finalDockerHost == [protocol: "https", host: "127.0.0.1", port: 2376]
    }

    def "should choose unix socket for 'unix:///var/run/socket.example'"() {
        def dockerUrlHandler = new DockerURLHandler()
        when:
        def finalDockerHost = dockerUrlHandler.getBaseURLWithActualProtocol("unix:///var/run/socket.example")
        then:
        finalDockerHost == [protocol: "unix", host: "/var/run/socket.example", port: -1]
    }

    def "should choose named pipe for 'npipe:////./pipe/docker_engine'"() {
        def dockerUrlHandler = new DockerURLHandler()
        when:
        def finalDockerHost = dockerUrlHandler.getBaseURLWithActualProtocol("npipe:////./pipe/docker_engine")
        then:
        finalDockerHost == [protocol: "npipe", host: "//./pipe/docker_engine", port: -1]
    }

    def "should ignore unknown protocol"() {
        def dockerUrlHandler = new DockerURLHandler()
        when:
        def finalDockerHost = dockerUrlHandler.getBaseURLWithActualProtocol("ftp://example/foo")
        then:
        finalDockerHost == [protocol: "ftp", host: "example", port: -1]
    }
}
