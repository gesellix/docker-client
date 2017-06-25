package de.gesellix.docker.engine

import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Paths

class DockerClientConfigSpec extends Specification {

    @Unroll
    def "should assume TLS when tlsVerify==#tlsVerify"() {
        def certsDir = Files.createTempDirectory("certs")
        def dockerEnv = new de.gesellix.docker.engine.DockerEnv(
                tlsVerify: tlsVerify,
                certPath: certsDir.toString())
        def dockerClientConfig = new de.gesellix.docker.engine.DockerClientConfig(dockerEnv)
        when:
        def tlsConfig = dockerClientConfig.getTlsConfig(new URL("https://example.com:2376"), dockerEnv)
        then:
        tlsConfig.tlsVerify
        and:
        tlsConfig.certPath == certsDir.toString()
        where:
        // yes, even the falsy values (0, false, no) actually enable tls-verify
        tlsVerify << ["1", "true", "yes", "0", "false", "no"]
    }

    @Unroll
    def "should not assume TLS when tlsVerify==#tlsVerify"() {
        def certsDir = Files.createTempDirectory("certs")
        def dockerEnv = new de.gesellix.docker.engine.DockerEnv(
                tlsVerify: tlsVerify,
                certPath: certsDir.toString())
        def dockerClientConfig = new de.gesellix.docker.engine.DockerClientConfig(dockerEnv)
        when:
        def assumeTls = dockerClientConfig.getTlsConfig(new URL("https://example.com:2376"), dockerEnv).tlsVerify
        then:
        !assumeTls
        and:
        dockerClientConfig.certPath == null
        where:
        tlsVerify << [""]
    }

    def "should not assume TLS when port !== 2376"() {
        def dockerEnv = new de.gesellix.docker.engine.DockerEnv(
                tlsVerify: null,
                certPath: "/some/non-existing/path")
        def dockerClientConfig = new de.gesellix.docker.engine.DockerClientConfig(dockerEnv)
        when:
        def assumeTls = dockerClientConfig.getTlsConfig(new URL("https://example.com:2375"), dockerEnv).tlsVerify
        then:
        !assumeTls
        and:
        dockerClientConfig.certPath == null
    }

    def "should fail when tlsVerify=1, but certs directory doesn't exist"() {
        def dockerEnv = new de.gesellix.docker.engine.DockerEnv(
                tlsVerify: "1",
                certPath: "/some/non-existing/path",
                defaultCertPath: new File("/some/non-existing/default/path"))
        def dockerClientConfig = new de.gesellix.docker.engine.DockerClientConfig(dockerEnv)
        when:
        dockerClientConfig.getTlsConfig(new URL("https://example.com:2375"), dockerEnv)
        then:
        thrown(IllegalStateException)
    }

    def "should try to use the default .docker cert path"() {
        def dockerEnv = new de.gesellix.docker.engine.DockerEnv(
                tlsVerify: null,
                certPath: "/some/non-existing/path")
        def defaultDockerCertPathExisted = Files.exists(Paths.get(dockerEnv.defaultCertPath))
        if (!defaultDockerCertPathExisted) {
            Files.createDirectory(Paths.get(dockerEnv.defaultCertPath))
        }
        def dockerClientConfig = new de.gesellix.docker.engine.DockerClientConfig(dockerEnv)

        when:
        def tlsConfig = dockerClientConfig.getTlsConfig(new URL("https://example.com:2376"), dockerEnv)

        then:
        tlsConfig.tlsVerify
        and:
        tlsConfig.certPath == dockerEnv.defaultCertPath

        cleanup:
        defaultDockerCertPathExisted || Files.delete(Paths.get(dockerEnv.defaultCertPath))
    }

    def "should choose http for 'tcp://127.0.0.1:2375'"() {
        def dockerEnv = new de.gesellix.docker.engine.DockerEnv(dockerHost: "tcp://127.0.0.1:2375", tlsVerify: null)
        when:
        def dockerClientConfig = new de.gesellix.docker.engine.DockerClientConfig(dockerEnv)
        then:
        dockerClientConfig.scheme == "http"
        dockerClientConfig.host == "127.0.0.1"
        dockerClientConfig.port == 2375
        dockerClientConfig.certPath == null
    }

    def "should choose http for 'http://127.0.0.1:2375'"() {
        def dockerEnv = new de.gesellix.docker.engine.DockerEnv(dockerHost: "http://127.0.0.1:2375", tlsVerify: null)
        when:
        def dockerClientConfig = new de.gesellix.docker.engine.DockerClientConfig(dockerEnv)
        then:
        dockerClientConfig.scheme == "http"
        dockerClientConfig.host == "127.0.0.1"
        dockerClientConfig.port == 2375
        dockerClientConfig.certPath == null
    }

    def "should choose http for 'https://127.0.0.1:2376' and disabled tls"() {
        def dockerEnv = new de.gesellix.docker.engine.DockerEnv(dockerHost: "https://127.0.0.1:2376", tlsVerify: "")
        when:
        def dockerClientConfig = new de.gesellix.docker.engine.DockerClientConfig(dockerEnv)
        then:
        dockerClientConfig.scheme == "http"
        dockerClientConfig.host == "127.0.0.1"
        dockerClientConfig.port == 2376
        dockerClientConfig.certPath == null
    }

    def "should choose https for 'https://127.0.0.1:2376' and enabled tls"() {
        def certsDir = Files.createTempDirectory("certs")
        def dockerEnv = new de.gesellix.docker.engine.DockerEnv(dockerHost: "https://127.0.0.1:2376", tlsVerify: "1", certPath: certsDir.toString())
        when:
        def dockerClientConfig = new de.gesellix.docker.engine.DockerClientConfig(dockerEnv)
        then:
        dockerClientConfig.scheme == "https"
        dockerClientConfig.host == "127.0.0.1"
        dockerClientConfig.port == 2376
        dockerClientConfig.certPath == certsDir.toString()
    }

    def "should choose unix socket for 'unix:///var/run/socket.example'"() {
        when:
        def dockerClientConfig = new de.gesellix.docker.engine.DockerClientConfig("unix:///var/run/socket.example")
        then:
        dockerClientConfig.scheme == "unix"
        dockerClientConfig.host == "/var/run/socket.example"
        dockerClientConfig.port == -1
        dockerClientConfig.certPath == null
    }

    def "should choose named pipe for 'npipe:////./pipe/docker_engine'"() {
        when:
        def dockerClientConfig = new de.gesellix.docker.engine.DockerClientConfig("npipe:////./pipe/docker_engine")
        then:
        dockerClientConfig.scheme == "npipe"
        dockerClientConfig.host == "//./pipe/docker_engine"
        dockerClientConfig.port == -1
        dockerClientConfig.certPath == null
    }

    def "should ignore unknown protocol"() {
        when:
        def dockerClientConfig = new de.gesellix.docker.engine.DockerClientConfig("ftp://example/foo")
        then:
        dockerClientConfig.scheme == "ftp"
        dockerClientConfig.host == "example"
        dockerClientConfig.port == -1
        dockerClientConfig.certPath == null
    }
}
