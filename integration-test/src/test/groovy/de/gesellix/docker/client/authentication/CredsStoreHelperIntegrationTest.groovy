package de.gesellix.docker.client.authentication

import de.gesellix.docker.engine.DockerEnv
import spock.lang.Requires
import spock.lang.Specification

class CredsStoreHelperIntegrationTest extends Specification {

  CredsStoreHelper helper

  def setup() {
    helper = new CredsStoreHelper()
    println "--- ${System.properties['user.name']} on ${System.properties['os.name']}"
  }

  @Requires({ System.properties['user.name'] == 'gesellix' && ['Mac OS X', 'Windows'].contains(System.properties['os.name']) })
  def "can get auth from desktop on Mac OS X and Windows"() {
    when:
    def result = helper.getAuthentication("desktop")
    then:
    result == new CredsStoreHelperResult(
        data: [
            ServerURL: new DockerEnv().indexUrl_v1,
            Username : "gesellix",
            Secret   : "-yet-another-password-"
        ]
    )
  }

  @Requires({ System.properties['user.name'] == 'gesellix' && ['Mac OS X', 'Windows'].contains(System.properties['os.name']) })
  def "can list auths from desktop on Mac OS X and Windows"() {
    when:
    def result = helper.getAllAuthentications("desktop")
    then:
    result == new CredsStoreHelperResult(data: [(new DockerEnv().indexUrl_v1): "gesellix"])
  }

  @Requires({ System.properties['user.name'] == 'gesellix' && System.properties['os.name'] == "Mac OS X" })
  def "can get auth from osxkeychain on Mac OS X"() {
    when:
    def result = helper.getAuthentication("osxkeychain")
    then:
    result == new CredsStoreHelperResult(
        data: [
            ServerURL: new DockerEnv().indexUrl_v1,
            Username : "gesellix",
            Secret   : "-yet-another-password-"
        ]
    )
  }

  @Requires({ System.properties['user.name'] == 'gesellix' && System.properties['os.name'] == "Mac OS X" })
  def "can list auths from osxkeychain on Mac OS X"() {
    when:
    def result = helper.getAllAuthentications("osxkeychain")
    then:
    result == new CredsStoreHelperResult(data: [(new DockerEnv().indexUrl_v1): "gesellix"])
  }

  @Requires({ System.properties['user.name'] == 'gesellix' && System.properties['os.name'] == "Windows" })
  def "can get auth from wincred on Windows"() {
    when:
    def result = helper.getAuthentication("wincred")
    then:
    result == new CredsStoreHelperResult(
        data: [
            ServerURL: new DockerEnv().indexUrl_v1,
            Username : "gesellix",
            Secret   : "-yet-another-password-"
        ]
    )
  }

  @Requires({ System.properties['user.name'] == 'gesellix' && System.properties['os.name'] == "Windows" })
  def "can list auths from wincred on Windows"() {
    when:
    def result = helper.getAllAuthentications("wincred")
    then:
    result == new CredsStoreHelperResult(data: [(new DockerEnv().indexUrl_v1): "gesellix"])
  }

  @Requires({ System.properties['user.name'] == 'gesellix' && System.properties['os.name'] == "Linux" })
  def "can get auth from secretservice on Linux"() {
    when:
    def result = helper.getAuthentication("secretservice")
    then:
    result == new CredsStoreHelperResult(
        error: null,
        data: [
            ServerURL: "",
            Username : "gesellix",
            Secret   : "-yet-another-password-"
        ]
    )
  }

  @Requires({ System.properties['user.name'] == 'gesellix' && System.properties['os.name'] == "Linux" })
  def "can list auths from secretservice on Linux"() {
    when:
    def result = helper.getAllAuthentications("secretservice")
    then:
    result == new CredsStoreHelperResult(data: [(new DockerEnv().indexUrl_v1): "gesellix"])
  }

  @Requires({ System.properties['user.name'] == 'gesellix' })
  def "handles errors more or less gracefully"() {
    when:
    def result = helper.getAuthentication(System.properties['os.name'] == "Linux" ? "secretservice" : "osxkeychain", "foo")
    then:
    result.data == null
    and:
    result.error =~ ".*credentials not found in native keychain.*"
  }

  def "handles missing docker-credential-helper more or less gracefully"() {
    when:
    def result = helper.getAuthentication("should-be-missing", "foo")
    then:
    result.data == null
    and:
    result.error =~ ".*Cannot run program \"docker-credential-should-be-missing\".*"
  }
}
