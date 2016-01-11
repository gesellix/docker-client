package de.gesellix.docker.client

import org.apache.commons.lang.SystemUtils
import spock.lang.Ignore
import spock.lang.IgnoreIf
import spock.lang.Specification

@IgnoreIf({ !System.env.DOCKER_HOST })
class LowLevelDockerClientIntegrationSpec extends Specification {

    def "should allow GET requests"() {
        def client = new LowLevelDockerClient(
                config: new DockerConfig(
                        dockerHost: System.env.DOCKER_HOST))
        expect:
        client.get("/_ping").content == "OK"
    }

    def "should allow POST requests"() {
        given:
        def client = new LowLevelDockerClient(
                config: new DockerConfig(
                        dockerHost: System.env.DOCKER_HOST))
        def request = [path : "/images/create",
                       query: [fromImage: "gesellix/docker-client-testimage",
                               tag      : "latest",
                               registry : ""]]

        when:
        def response = client.post(request)
        then:
        response.content.last() == [status: "Status: Image is up to date for gesellix/docker-client-testimage:latest"]
    }

    @Ignore("the password needs to be set before running this test")
    def "should allow POST requests with body"() {
        given:
        def client = new LowLevelDockerClient(
                config: new DockerConfig(
                        dockerHost: System.env.DOCKER_HOST))
        def authDetails = ["username"     : "gesellix",
                           "password"     : "-yet-another-password-",
                           "email"        : "tobias@gesellix.de",
                           "serveraddress": "https://index.docker.io/v1/"]
        def request = [path       : "/auth",
                       body       : authDetails,
                       contentType: "application/json"]
        when:
        def response = client.post(request)
        then:
        response.content == [Status: "Login Succeeded"]
    }

    def "should optionally stream a response"() {
        def client = new LowLevelDockerClient(
                config: new DockerConfig(
                        dockerHost: System.env.DOCKER_HOST))
        def outputStream = new ByteArrayOutputStream()
        when:
        client.get([path  : "/_ping",
                    stdout: outputStream])
        then:
        outputStream.toString() == "OK"
    }

    def "should parse application/json"() {
        def client = new LowLevelDockerClient(
                config: new DockerConfig(
                        dockerHost: System.env.DOCKER_HOST))
        when:
        def response = client.get("/version")
        then:
        def content = response.content
        content.ApiVersion == "1.21"
        content.Arch == "amd64"
        content.GitCommit == "a34a1d5"
        content.GoVersion == "go1.4.3"
        content.KernelVersion =~ "\\d.\\d{1,2}.\\d{1,2}-\\w+"
        content.Os == "linux"
        content.Version == "1.9.1"
    }

    @IgnoreIf({ !SystemUtils.IS_OS_LINUX || !new File("/var/run/docker.sock").exists() })
    def "should support unix socket connections"() {
        def client = new LowLevelDockerClient(
                config: new DockerConfig(
                        dockerHost: "unix:///var/run/docker.sock"))
        when:
        def response = client.request([method: "GET",
                                       path  : "/info"])
        then:
        response.status.code == 200
    }
}
