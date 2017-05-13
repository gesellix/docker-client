package de.gesellix.docker.client.authentication

import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.client.HttpClient
import de.gesellix.docker.client.config.DockerEnv
import de.gesellix.docker.testutil.ResourceReader
import spock.lang.Ignore
import spock.lang.Specification

class ManageAuthenticationClientTest extends Specification {

    DockerEnv env
    HttpClient client
    ManageAuthenticationClient service

    def setup() {
        env = Mock(DockerEnv)
        client = Mock(HttpClient)
        service = Spy(ManageAuthenticationClient, constructorArgs: [
                env,
                client,
                Mock(DockerResponseHandler)])
    }

    def "read and encode authConfig (old format)"() {
        given:
        env.indexUrl_v1 >> 'https://index.docker.io/v1/'
        def dockerCfg = new ResourceReader().getClasspathResourceAsFile('/auth/dockercfg', DockerClient)
        def authDetails = service.readAuthConfig(null, dockerCfg)
        def authPlain = authDetails

        when:
        def authResult = service.encodeAuthConfig(authPlain)

        then:
        authResult == 'eyJ1c2VybmFtZSI6Imdlc2VsbGl4IiwicGFzc3dvcmQiOiIteWV0LWFub3RoZXItcGFzc3dvcmQtIiwiZW1haWwiOiJ0b2JpYXNAZ2VzZWxsaXguZGUiLCJzZXJ2ZXJhZGRyZXNzIjoiaHR0cHM6Ly9pbmRleC5kb2NrZXIuaW8vdjEvIn0='
    }

    def "read and encode authConfig (new format)"() {
        given:
        env.indexUrl_v1 >> 'https://index.docker.io/v1/'
        def dockerCfg = new ResourceReader().getClasspathResourceAsFile('/auth/config.json', DockerClient)
        def authDetails = service.readAuthConfig(null, dockerCfg)
        def authPlain = authDetails

        when:
        def authResult = service.encodeAuthConfig(authPlain)

        then:
        authResult == 'eyJ1c2VybmFtZSI6Imdlc2VsbGl4IiwicGFzc3dvcmQiOiIteWV0LWFub3RoZXItcGFzc3dvcmQtIiwiZW1haWwiOiJ0b2JpYXNAZ2VzZWxsaXguZGUiLCJzZXJ2ZXJhZGRyZXNzIjoiaHR0cHM6Ly9pbmRleC5kb2NrZXIuaW8vdjEvIn0='
    }

    def "login"() {
        def authDetails = [:]
        when:
        service.auth(authDetails)

        then:
        1 * client.post([path              : "/auth",
                         body              : authDetails,
                         requestContentType: "application/json"])
    }

    def "read auth config for official Docker index"() {
        given:
        env.indexUrl_v1 >> 'https://index.docker.io/v1/'
        def dockerCfg = new ResourceReader().getClasspathResourceAsFile('/auth/config.json', DockerClient)

        when:
        def authDetails = service.readAuthConfig(null, dockerCfg)

        then:
        authDetails.username == "gesellix"
        and:
        authDetails.password == "-yet-another-password-"
        and:
        authDetails.email == "tobias@gesellix.de"
        and:
        authDetails.serveraddress == "https://index.docker.io/v1/"
    }

    def "read auth config for quay.io"() {
        given:
        def dockerCfg = new ResourceReader().getClasspathResourceAsFile('/auth/config.json', DockerClient)

        when:
        def authDetails = service.readAuthConfig("quay.io", dockerCfg)

        then:
        authDetails.username == "gesellix"
        and:
        authDetails.password == "-a-password-for-quay-"
        and:
        authDetails.email == "tobias@gesellix.de"
        and:
        authDetails.serveraddress == "quay.io"
    }

    def "read auth config for missing config file"() {
        given:
        def nonExistingFile = new File('./I should not exist')
        assert !nonExistingFile.exists()

        when:
        def authDetails = service.readAuthConfig(null, nonExistingFile)

        then:
        authDetails == [:]
    }

    def "read auth config for unknown registry hostname"() {
        given:
        def dockerCfg = new ResourceReader().getClasspathResourceAsFile('/auth/config.json', DockerClient)

        when:
        def authDetails = service.readAuthConfig("unkown.example.com", dockerCfg)

        then:
        authDetails == [:]
    }

    @Ignore("Needs https://github.com/gesellix/docker-client/issues/41")
    def "read default docker config file using credsStore"() {
        given:
        def oldDockerConfig = System.clearProperty("docker.config")
        def expectedConfigFile = new ResourceReader().getClasspathResourceAsFile('/auth/dockercfg-with-credsStore', DockerClient)
        env.indexUrl_v1 >> 'https://index.docker.io/v1/'
        env.getDockerConfigFile() >> expectedConfigFile

        when:
        def result = service.readDefaultAuthConfig()

        then:
        1 * service.readAuthConfig(null, expectedConfigFile)
        result == ["username"     : "gesellix",
                   "password"     : "-yet-another-password-",
                   "email"        : "tobias@gesellix.de",
                   "serveraddress": "https://index.docker.io/v1/"]

        cleanup:
        if (oldDockerConfig) {
            System.setProperty("docker.config", oldDockerConfig)
        }
    }

    def "read default docker config file"() {
        given:
        def oldDockerConfig = System.clearProperty("docker.config")
        def expectedConfigFile = new ResourceReader().getClasspathResourceAsFile('/auth/config.json', DockerClient)
        env.indexUrl_v1 >> 'https://index.docker.io/v1/'
        env.getDockerConfigFile() >> expectedConfigFile

        when:
        def result = service.readDefaultAuthConfig()

        then:
        1 * service.readAuthConfig(null, expectedConfigFile)
        result == ["username"     : "gesellix",
                   "password"     : "-yet-another-password-",
                   "email"        : "tobias@gesellix.de",
                   "serveraddress": "https://index.docker.io/v1/"]

        cleanup:
        if (oldDockerConfig) {
            System.setProperty("docker.config", oldDockerConfig)
        }
    }

    def "read legacy docker config file"() {
        given:
        def oldDockerConfig = System.clearProperty("docker.config")
        def expectedConfigFile = new ResourceReader().getClasspathResourceAsFile('/auth/dockercfg', DockerClient)
        env.indexUrl_v1 >> 'https://index.docker.io/v1/'

        when:
        def result = service.readAuthConfig(null, expectedConfigFile)

        then:
        result == ["username"     : "gesellix",
                   "password"     : "-yet-another-password-",
                   "email"        : "tobias@gesellix.de",
                   "serveraddress": "https://index.docker.io/v1/"]

        cleanup:
        if (oldDockerConfig) {
            System.setProperty("docker.config", oldDockerConfig)
        }
    }
}
