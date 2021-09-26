package de.gesellix.docker.client.authentication

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.system.ManageSystem
import de.gesellix.docker.engine.DockerEnv
import de.gesellix.docker.engine.EngineClient
import de.gesellix.testutil.ResourceReader
import spock.lang.Requires
import spock.lang.Specification

import java.lang.reflect.Type

import static de.gesellix.docker.client.authentication.AuthConfig.EMPTY_AUTH_CONFIG

class ManageAuthenticationClientTest extends Specification {

  DockerEnv env
  ManageAuthenticationClient service

  Moshi moshi = new Moshi.Builder().build()

  def setup() {
    env = Mock(DockerEnv)
    service = new ManageAuthenticationClient(env, Mock(EngineClient), Mock(ManageSystem))
    service.authConfigReader = Spy(AuthConfigReader, constructorArgs: [env])
  }

  def "read authConfig (new format)"() {
    given:
    def oldDockerConfig = System.clearProperty("docker.config")
    def expectedConfigFile = new ResourceReader().getClasspathResourceAsFile('/auth/config.json', DockerClient)
    env.indexUrl_v1 >> 'https://index.docker.io/v1/'

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
    def oldDockerConfig = System.clearProperty("docker.config")
    def expectedConfigFile = new ResourceReader().getClasspathResourceAsFile('/auth/dockercfg', DockerClient)
    env.indexUrl_v1 >> 'https://index.docker.io/v1/'

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
  def "read all auth configs"() {
    given:
    def oldDockerConfig = System.clearProperty("docker.config")
    def configFile = "/auth/dockercfg-with-credsStore-${System.properties['os.name'].toString().toLowerCase().capitalize().replaceAll("\\s", "_")}"
    def expectedConfigFile = new ResourceReader().getClasspathResourceAsFile(configFile, DockerClient)
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
    def authResult = service.encodeAuthConfig(expectedAuthConfig)

    then:
    moshi.adapter(AuthConfig).fromJson(new String(authResult.decodeBase64())) == expectedAuthConfig
  }

  def "encode a Map of authConfigs"() {
    given:
    def expectedAuthConfigs = ["for-test": new AuthConfig(username: "user", password: "secret")]

    when:
    def authResult = service.encodeAuthConfigs(expectedAuthConfigs)

    then:
    Type type = Types.newParameterizedType(Map, String, AuthConfig)
    moshi.adapter(type).fromJson(new String(authResult.decodeBase64())) == expectedAuthConfigs
  }

  def "login"() {
    def authDetails = [:]
    when:
    service.auth(authDetails)

    then:
    1 * service.client.post([path              : "/auth",
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
    authDetails == new AuthConfig()
  }

  def "read auth config for unknown registry hostname"() {
    given:
    def dockerCfg = new ResourceReader().getClasspathResourceAsFile('/auth/config.json', DockerClient)

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
    def oldDockerConfig = System.clearProperty("docker.config")
    def expectedConfigFile = new ResourceReader().getClasspathResourceAsFile('/auth/config.json', DockerClient)
    env.indexUrl_v1 >> 'https://index.docker.io/v1/'
    env.getDockerConfigFile() >> expectedConfigFile

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
