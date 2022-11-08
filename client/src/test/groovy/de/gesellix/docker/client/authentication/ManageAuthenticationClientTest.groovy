package de.gesellix.docker.client.authentication

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import de.gesellix.docker.authentication.AuthConfig
import de.gesellix.docker.authentication.AuthConfigReader
import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.engine.DockerConfigReader
import de.gesellix.docker.engine.DockerEnv
import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.SystemAuthResponse
import de.gesellix.docker.remote.api.client.SystemApi
import de.gesellix.testutil.ResourceReader
import spock.lang.Requires
import spock.lang.Specification

import java.lang.reflect.Type

import static de.gesellix.docker.authentication.AuthConfig.EMPTY_AUTH_CONFIG

class ManageAuthenticationClientTest extends Specification {

  DockerEnv env
  ManageAuthenticationClient service

  Moshi moshi = new Moshi.Builder().build()

  def setup() {
    def dockerConfigReader = Mock(DockerConfigReader)
    env = Mock(DockerEnv)
    env.getDockerConfigReader() >> dockerConfigReader
    service = new ManageAuthenticationClient(Mock(EngineApiClient), new AuthConfigReader(env), dockerConfigReader)
    service.authConfigReader = Spy(AuthConfigReader, constructorArgs: [env])
  }

  def "read authConfig (new format)"() {
    given:
    env.indexUrl_v1 >> 'https://index.docker.io/v1/'
    String oldDockerConfig = System.clearProperty("docker.config")
    File expectedConfigFile = new ResourceReader().getClasspathResourceAsFile('/auth/config.json', DockerClient)
    env.getDockerConfigReader().getDockerConfigFile() >> expectedConfigFile
    env.getDockerConfigReader().readDockerConfigFile(expectedConfigFile) >> [
        auths: [
            "https://index.docker.io/v1/": [
                auth : "Z2VzZWxsaXg6LXlldC1hbm90aGVyLXBhc3N3b3JkLQ",
                email: "tobias@gesellix.de"
            ],
            "quay.io"                    : [
                auth : "Z2VzZWxsaXg6LWEtcGFzc3dvcmQtZm9yLXF1YXkt",
                email: "tobias@gesellix.de"
            ]
        ]
    ]

    when:
    def result = service.readAuthConfig(null, expectedConfigFile)

    then:
    result == new AuthConfig(username: "gesellix",
        password: "-yet-another-password-",
        email: "tobias@gesellix.de",
        serveraddress: "https://index.docker.io/v1/")

    cleanup:
    if (oldDockerConfig) {
      System.setProperty("docker.config", oldDockerConfig)
    }
  }

  def "read authConfig (legacy format)"() {
    given:
    env.indexUrl_v1 >> 'https://index.docker.io/v1/'
    String oldDockerConfig = System.clearProperty("docker.config")
    File expectedConfigFile = new ResourceReader().getClasspathResourceAsFile('/auth/dockercfg', DockerClient)
    env.getDockerConfigReader().getDockerConfigFile() >> expectedConfigFile
    env.getDockerConfigReader().readDockerConfigFile(expectedConfigFile) >> [
        "https://index.docker.io/v1/": [
            auth : "Z2VzZWxsaXg6LXlldC1hbm90aGVyLXBhc3N3b3JkLQ==",
            email: "tobias@gesellix.de"
        ],
        "quay.io"                    : [
            auth : "Z2VzZWxsaXg6LWEtcGFzc3dvcmQtZm9yLXF1YXkt",
            email: "tobias@gesellix.de"
        ]
    ]

    when:
    def result = service.readAuthConfig(null, expectedConfigFile)

    then:
    result == new AuthConfig(username: "gesellix",
        password: "-yet-another-password-",
        email: "tobias@gesellix.de",
        serveraddress: "https://index.docker.io/v1/")

    cleanup:
    if (oldDockerConfig) {
      System.setProperty("docker.config", oldDockerConfig)
    }
  }

  @Requires({ System.properties['user.name'] == 'gesellix' })
  def "read all auth configs (deprecated)"() {
    given:
    String oldDockerConfig = System.clearProperty("docker.config")
    String configFile = "/auth/dockercfg-with-credsStore-${System.properties['os.name'].toString().toLowerCase().capitalize().replaceAll("\\s", "_")}"
    File expectedConfigFile = new ResourceReader().getClasspathResourceAsFile(configFile, DockerClient)
    env.indexUrl_v1 >> 'https://index.docker.io/v1/'

    when:
    Map<String, AuthConfig> result = service.getAllAuthConfigs(expectedConfigFile)

    then:
    result.size() == 1
    AuthConfig authConfig = result["https://index.docker.io/v1/"]
    authConfig.serveraddress == "https://index.docker.io/v1/"
    authConfig.username == "gesellix"
    authConfig.password =~ ".+"

    cleanup:
    if (oldDockerConfig) {
      System.setProperty("docker.config", oldDockerConfig)
    }
  }

  @Requires({ System.properties['user.name'] == 'gesellix' })
  def "read all auth configs"() {
    given:
    String oldDockerConfig = System.clearProperty("docker.config")
    String configFile = "/auth/dockercfg-with-credsStore-${System.properties['os.name'].toString().toLowerCase().capitalize().replaceAll("\\s", "_")}"
    File expectedConfigFile = new ResourceReader().getClasspathResourceAsFile(configFile, DockerClient)
    env.indexUrl_v1 >> 'https://index.docker.io/v1/'

    when:
    Map<String, AuthConfig> result = service.getAllAuthConfigs(expectedConfigFile)

    then:
    result.size() == 1
    AuthConfig authConfig = result["https://index.docker.io/v1/"]
    authConfig.serveraddress == "https://index.docker.io/v1/"
    authConfig.username == "gesellix"
    authConfig.password =~ ".+"

    cleanup:
    if (oldDockerConfig) {
      System.setProperty("docker.config", oldDockerConfig)
    }
  }

  def "encode a single authConfig"() {
    given:
    def expectedAuthConfig = new AuthConfig(username: "gesellix", password: "-yet-another-password-", email: "tobias@gesellix.de", serveraddress: "https://index.docker.io/v1/")

    when:
    String authResult = service.encodeAuthConfig(expectedAuthConfig)

    then:
    moshi.adapter(AuthConfig).fromJson(new String(authResult.decodeBase64())) == expectedAuthConfig
  }

  def "encode a Map of authConfigs"() {
    given:
    def expectedAuthConfigs = ["for-test": new AuthConfig(username: "user", password: "secret")]

    when:
    String authResult = service.encodeAuthConfigs(expectedAuthConfigs)

    then:
    Type type = Types.newParameterizedType(Map, String, AuthConfig)
    moshi.adapter(type).fromJson(new String(authResult.decodeBase64())) == expectedAuthConfigs
  }

  def "login"() {
    given:
    def systemApi = Mock(SystemApi)
    service.client.systemApi >> systemApi
    def authConfig = new de.gesellix.docker.remote.api.AuthConfig()
    def authResponse = Mock(SystemAuthResponse)

    when:
    def auth = service.auth(authConfig)

    then:
    1 * systemApi.systemAuth(authConfig) >> authResponse
    auth.content == authResponse
  }

  def "read auth config for official Docker index"() {
    given:
    env.indexUrl_v1 >> 'https://index.docker.io/v1/'
    File dockerCfg = new ResourceReader().getClasspathResourceAsFile('/auth/config.json', DockerClient)
    env.getDockerConfigReader().getDockerConfigFile() >> dockerCfg
    env.getDockerConfigReader().readDockerConfigFile(dockerCfg) >> [
        auths: [
            "https://index.docker.io/v1/": [
                auth : "Z2VzZWxsaXg6LXlldC1hbm90aGVyLXBhc3N3b3JkLQ",
                email: "tobias@gesellix.de"
            ],
            "quay.io"                    : [
                auth : "Z2VzZWxsaXg6LWEtcGFzc3dvcmQtZm9yLXF1YXkt",
                email: "tobias@gesellix.de"
            ]
        ]
    ]

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
    File dockerCfg = new ResourceReader().getClasspathResourceAsFile('/auth/config.json', DockerClient)
    env.getDockerConfigReader().getDockerConfigFile() >> dockerCfg
    env.getDockerConfigReader().readDockerConfigFile(dockerCfg) >> [
        auths: [
            "https://index.docker.io/v1/": [
                auth : "Z2VzZWxsaXg6LXlldC1hbm90aGVyLXBhc3N3b3JkLQ",
                email: "tobias@gesellix.de"
            ],
            "quay.io"                    : [
                auth : "Z2VzZWxsaXg6LWEtcGFzc3dvcmQtZm9yLXF1YXkt",
                email: "tobias@gesellix.de"
            ]
        ]
    ]

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
    authDetails == new AuthConfig()
  }

  def "read auth config for unknown registry hostname"() {
    given:
    File dockerCfg = new ResourceReader().getClasspathResourceAsFile('/auth/config.json', DockerClient)

    when:
    def authDetails = service.readAuthConfig("unknown.example.com", dockerCfg)

    then:
    authDetails == EMPTY_AUTH_CONFIG
  }

  @Requires({ System.properties['user.name'] == 'gesellix' })
  def "read default docker config file using credsStore"() {
    given:
    String oldDockerConfig = System.clearProperty("docker.config")
    String configFile = "/auth/dockercfg-with-credsStore-${System.properties['os.name'].toString().toLowerCase().capitalize().replaceAll("\\s", "_")}"
    File expectedConfigFile = new ResourceReader().getClasspathResourceAsFile(configFile, DockerClient)
    env.indexUrl_v1 >> 'https://index.docker.io/v1/'
    env.getDockerConfigFile() >> expectedConfigFile

    when:
    def authConfig = service.readDefaultAuthConfig()

    then:
    1 * service.authConfigReader.readAuthConfig(null, expectedConfigFile)
    authConfig.serveraddress == "https://index.docker.io/v1/"
    authConfig.username == "gesellix"
    authConfig.password =~ ".+"

    cleanup:
    if (oldDockerConfig) {
      System.setProperty("docker.config", oldDockerConfig)
    }
  }

  def "read default authConfig"() {
    given:
    String oldDockerConfig = System.clearProperty("docker.config")
    File expectedConfigFile = new ResourceReader().getClasspathResourceAsFile('/auth/config.json', DockerClient)
    env.indexUrl_v1 >> 'https://index.docker.io/v1/'
    env.getDockerConfigFile() >> expectedConfigFile
    env.getDockerConfigReader().getDockerConfigFile() >> expectedConfigFile
    env.getDockerConfigReader().readDockerConfigFile(expectedConfigFile) >> [
        auths: [
            "https://index.docker.io/v1/": [
                auth : "Z2VzZWxsaXg6LXlldC1hbm90aGVyLXBhc3N3b3JkLQ",
                email: "tobias@gesellix.de"
            ],
            "quay.io"                    : [
                auth : "Z2VzZWxsaXg6LWEtcGFzc3dvcmQtZm9yLXF1YXkt",
                email: "tobias@gesellix.de"
            ]
        ]
    ]

    when:
    def result = service.readDefaultAuthConfig()

    then:
    1 * service.authConfigReader.readAuthConfig(null, expectedConfigFile)
    result == new AuthConfig(username: "gesellix",
        password: "-yet-another-password-",
        email: "tobias@gesellix.de",
        serveraddress: "https://index.docker.io/v1/")

    cleanup:
    if (oldDockerConfig) {
      System.setProperty("docker.config", oldDockerConfig)
    }
  }
}
