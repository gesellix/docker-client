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

    @Requires({ System.properties['user.name'] == 'gesellix' && System.properties['os.name'] == "Mac OS X" })
    def "can retrieve auth from osxkeychain on Mac OS X"() {
        when:
        def result = helper.getAuthentication("osxkeychain")
        then:
        result == [
                auth: [
                        ServerURL: new DockerEnv().indexUrl_v1,
                        Username : "gesellix",
                        Secret   : "-yet-another-password-"
                ]
        ]
    }

    @Requires({ System.properties['user.name'] == 'gesellix' && System.properties['os.name'] == "Windows" })
    def "can retrieve auth from wincred on Windows"() {
        when:
        def result = helper.getAuthentication("wincred")
        then:
        result == [
                auth: [
                        ServerURL: new DockerEnv().indexUrl_v1,
                        Username : "gesellix",
                        Secret   : "-yet-another-password-"
                ]
        ]
    }

    @Requires({ System.properties['user.name'] == 'gesellix' && System.properties['os.name'] == "Linux" })
    def "can retrieve auth from secretservice on Linux"() {
        when:
        def result = helper.getAuthentication("secretservice")
        then:
        result == [
                auth: [
                        ServerURL: new DockerEnv().indexUrl_v1,
                        Username : "gesellix",
                        Secret   : "-yet-another-password-"
                ]
        ]
    }

    @Requires({ System.properties['user.name'] == 'gesellix' })
    def "handles errors more or less gracefully"() {
        when:
        def result = helper.getAuthentication("osxkeychain", "foo")
        then:
        result.auth == null
        and:
        result.error =~ ".*credentials not found in native keychain.*"
    }

    def "handles missing docker-credential-helper more or less gracefully"() {
        when:
        def result = helper.getAuthentication("should-be-missing", "foo")
        then:
        result.auth == null
        and:
        result.error =~ ".*Cannot run program \"docker-credential-should-be-missing\".*"
    }
}
