package de.gesellix.docker.client

import spock.lang.Ignore
import spock.lang.Requires
import spock.lang.Specification

@Requires({ LocalDocker.available() })
class HttpClientIntegrationSpec extends Specification {

    def "should allow GET requests"() {
        def client = new HttpClient()
        expect:
        client.get("/_ping").content == "OK"
    }

    def "should allow POST requests"() {
        given:
        def client = new HttpClient()
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
        def client = new HttpClient()
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
        def client = new HttpClient()
        def outputStream = new ByteArrayOutputStream()
        when:
        client.get([path  : "/_ping",
                    stdout: outputStream])
        then:
        outputStream.toString() == "OK"
    }

    def "should parse application/json"() {
        def client = new HttpClient()
        when:
        def response = client.get("/version")
        then:
        def content = response.content
        content.ApiVersion == "1.23"
        content.Arch == "amd64"
        content.GitCommit == "9b9022a"
        content.GoVersion == "go1.5.3"
        content.KernelVersion =~ "\\d.\\d{1,2}.\\d{1,2}(-\\w+)?"
        content.Os == "linux"
        content.Version == "1.11.0-rc3"
    }

    @Requires({ new File("/var/run/docker.sock").exists() })
    def "should support unix socket connections (Linux native or Docker for Mac/Windows)"() {
        def client = new HttpClient(
                config: new DockerConfig(
                        dockerHost: "unix:///var/run/docker.sock"))
        when:
        def response = client.request([method: "GET",
                                       path  : "/info"])
        then:
        response.status.code == 200
    }
}
