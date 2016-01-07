package de.gesellix.docker.client.protocolhandler

import de.gesellix.docker.client.DockerConfig
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files

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
        tlsVerify << ["1", "true", "yes"]
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
        tlsVerify << ["0", "false", "no"]
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
        def defaultDockerCertPathExisted = Files.exists(dockerUrlHandler.config.defaultCertPath.toPath())
        if (!defaultDockerCertPathExisted) {
            Files.createDirectory(dockerUrlHandler.config.defaultCertPath.toPath())
        }
        when:
        def assumeTls = dockerUrlHandler.shouldUseTls(new URL("https://example.com:2376"))
        then:
        assumeTls
        cleanup:
        defaultDockerCertPathExisted || Files.delete(dockerUrlHandler.config.defaultCertPath.toPath())
    }

    def "should choose http for 'tcp://127.0.0.1:2375'"() {
        def dockerUrlHandler = new DockerURLHandler(
                config: new DockerConfig(
                        tlsVerify: null))
        when:
        def finalDockerHost = dockerUrlHandler.getURLWithActualProtocol("tcp://127.0.0.1:2375")
        then:
        finalDockerHost.toString() == "http://127.0.0.1:2375"
    }

    def "should choose http for 'http://127.0.0.1:2375'"() {
        def dockerUrlHandler = new DockerURLHandler(
                config: new DockerConfig(
                        tlsVerify: null))
        when:
        def finalDockerHost = dockerUrlHandler.getURLWithActualProtocol("http://127.0.0.1:2375")
        then:
        finalDockerHost.toString() == "http://127.0.0.1:2375"
    }

    def "should choose http for 'https://127.0.0.1:2376' and disabled tls"() {
        def dockerUrlHandler = new DockerURLHandler(
                config: new DockerConfig(
                        tlsVerify: "0"))
        when:
        def finalDockerHost = dockerUrlHandler.getURLWithActualProtocol("https://127.0.0.1:2376")
        then:
        finalDockerHost.toString() == "http://127.0.0.1:2376"
    }

    def "should choose https for 'https://127.0.0.1:2376' and enabled tls"() {
        def certsDir = Files.createTempDirectory("certs")
        def dockerUrlHandler = new DockerURLHandler(
                config: new DockerConfig(
                        tlsVerify: "1",
                        certPath: certsDir.toString()))
        when:
        def finalDockerHost = dockerUrlHandler.getURLWithActualProtocol("https://127.0.0.1:2376")
        then:
        finalDockerHost.toString() == "https://127.0.0.1:2376"
    }

    def "should choose unix socket for 'unix:///var/lib/socket.example'"() {
        def dockerUrlHandler = new DockerURLHandler()
        when:
        def finalDockerHost = dockerUrlHandler.getURLWithActualProtocol("unix:///var/lib/socket.example")
        then:
        finalDockerHost.toString() == "unix://socket/var/lib/socket.example"
    }

    def "should ignore unknown protocol"() {
        def dockerUrlHandler = new DockerURLHandler()
        when:
        def finalDockerHost = dockerUrlHandler.getURLWithActualProtocol("ftp://example/foo")
        then:
        finalDockerHost.toString() == "ftp://example/foo"
    }
}
