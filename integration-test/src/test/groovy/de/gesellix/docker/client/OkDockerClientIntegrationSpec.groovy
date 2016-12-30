package de.gesellix.docker.client

import spock.lang.IgnoreIf
import spock.lang.Requires
import spock.lang.Specification

import static de.gesellix.docker.client.TestimageConstants.CONSTANTS

@Requires({ LocalDocker.available() })
class OkDockerClientIntegrationSpec extends Specification {

    final static dockerHubUsername = "gesellix"
    final static dockerHubPassword = "-yet-another-password-"
    final static dockerHubEmail = "tobias@gesellix.de"

    def "should allow GET requests"() {
        def client = new OkDockerClient()
        expect:
        client.get([path: "/_ping"]).content == "OK"
    }

    def "should allow POST requests"() {
        given:
        def client = new OkDockerClient()
        def request = [path : "/images/create",
                       query: [fromImage: de.gesellix.docker.client.TestimageConstants.CONSTANTS.imageRepo,
                               tag      : de.gesellix.docker.client.TestimageConstants.CONSTANTS.imageTag,
                               registry : ""]]

        when:
        def response = client.post(request)
        then:
        response.content.last() == [status: "Status: Image is up to date for ${de.gesellix.docker.client.TestimageConstants.CONSTANTS.imageName}".toString()]
    }

    @IgnoreIf({ dockerHubPassword == "-yet-another-password-" })
    "should allow POST requests with body"() {
        given:
        def client = new OkDockerClient()
        def authDetails = ["username"     : dockerHubUsername,
                           "password"     : dockerHubPassword,
                           "email"        : dockerHubEmail,
                           "serveraddress": "https://index.docker.io/v1/"]
        def request = [path              : "/auth",
                       body              : authDetails,
                       requestContentType: "application/json"]
        when:
        def response = client.post(request)
        then:
        response.content == [Status: "Login Succeeded"]
    }

    def "should optionally stream a response"() {
        def client = new OkDockerClient()
        def outputStream = new ByteArrayOutputStream()
        when:
        client.get([path  : "/_ping",
                    stdout: outputStream])
        then:
        outputStream.toString() == "OK"
    }

    def "should parse application/json"() {
        def client = new OkDockerClient()
        when:
        def response = client.get([path: "/version"])
        then:
        def content = response.content
        content.ApiVersion == "1.25"
        content.Arch == "amd64"
        content.GitCommit == "88862e7"
        content.GoVersion == "go1.7.3"
        content.KernelVersion =~ "\\d.\\d{1,2}.\\d{1,2}(-\\w+)?"
        content.MinAPIVersion == "1.12"
        content.Os == "linux"
        content.Version == "1.13.0-rc4"
    }

    @Requires({ LocalDocker.isUnixSocket() })
    "should support unix socket connections (Linux native or Docker for Mac)"() {
        def client = new OkDockerClient("unix:///var/run/docker.sock")
        when:
        def response = client.request([method: "GET",
                                       path  : "/info"])
        then:
        response.status.code == 200
    }

    @Requires({ LocalDocker.isNamedPipe() })
    "should support named pipe socket connections (Docker for Windows)"() {
        def client = new OkDockerClient("npipe:////./pipe/docker_engine")
        when:
        def response = client.request([method: "GET",
                                       path  : "/info"])
        then:
        response.status.code == 200
    }
}
