package de.gesellix.docker.client.authentication

import spock.lang.Specification
import spock.lang.Unroll

import static de.gesellix.docker.client.authentication.AuthConfig.EMPTY_AUTH_CONFIG

class NativeStoreTest extends Specification {

    NativeStore credsStore
    CredsStoreHelper credsStoreHelper = Mock(CredsStoreHelper)

    def setup() {
        credsStore = new NativeStore("test-helper")
        credsStore.credsStoreHelper = credsStoreHelper
    }

    def "getAuthConfig calls credsStoreHelper"() {
        when:
        credsStore.getAuthConfig("host.name")
        then:
        1 * credsStoreHelper.getAuthentication("test-helper", "host.name") >> [:]
    }

    @Unroll
    def "getAuthConfig returns empty AuthConfig on invalid credsStoreHelperResponse #credsStoreHelperResponse"() {
        given:
        credsStoreHelper.getAuthentication("test-helper", "host.name") >> credsStoreHelperResponse
        when:
        AuthConfig result = credsStore.getAuthConfig("host.name")
        then:
        result == EMPTY_AUTH_CONFIG
        where:
        credsStoreHelperResponse << [new CredsStoreHelperResult(error: "for-test"), new CredsStoreHelperResult()]
    }

    def "getAuthConfig returns AuthConfig for valid username/password credentials"() {
        given:
        def credsStoreHelperResponse = new CredsStoreHelperResult(data: [Username: "foo", Secret: "bar"])
        credsStoreHelper.getAuthentication("test-helper", "host.name") >> credsStoreHelperResponse

        when:
        AuthConfig result = credsStore.getAuthConfig("host.name")

        then:
        result == new AuthConfig(username: "foo", password: "bar", serveraddress: "host.name")
    }

    def "getAuthConfig returns AuthConfig for valid identitytoken credentials"() {
        given:
        def credsStoreHelperResponse = new CredsStoreHelperResult(data: [Username: "<token>", Secret: "baz"])
        credsStoreHelper.getAuthentication("test-helper", "host.name") >> credsStoreHelperResponse

        when:
        AuthConfig result = credsStore.getAuthConfig("host.name")

        then:
        result == new AuthConfig(identitytoken: "baz", serveraddress: "host.name")
    }

    def "getAuthConfigs calls credsStoreHelper"() {
        when:
        credsStore.getAuthConfigs()
        then:
        1 * credsStoreHelper.getAllAuthentications("test-helper") >> [:]
    }

    @Unroll
    def "getAuthConfigs returns empty AuthConfig on invalid credsStoreHelperResponse #credsStoreHelperResponse"() {
        when:
        Map<String, AuthConfig> result = credsStore.getAuthConfigs()

        then:
        1 * credsStoreHelper.getAllAuthentications("test-helper") >> new CredsStoreHelperResult(data: ["host1.name": "username", "host2.name": "username"])
        1 * credsStoreHelper.getAuthentication("test-helper", "host1.name") >> new CredsStoreHelperResult(data: [Username: "<token>", Secret: "baz"])
        1 * credsStoreHelper.getAuthentication("test-helper", "host2.name") >> credsStoreHelperResponse
        0 * credsStoreHelper._
        result.size() == 2
        result["host2.name"] == EMPTY_AUTH_CONFIG

        where:
        credsStoreHelperResponse << [new CredsStoreHelperResult(error: "for-test"), new CredsStoreHelperResult()]
    }

    def "getAuthConfigs returns AuthConfigs for valid credsStoreHelperResponse"() {
        when:
        Map<String, AuthConfig> result = credsStore.getAuthConfigs()

        then:
        1 * credsStoreHelper.getAllAuthentications("test-helper") >> new CredsStoreHelperResult(data: ["host1.name": "username", "host2.name": "username"])
        1 * credsStoreHelper.getAuthentication("test-helper", "host1.name") >> new CredsStoreHelperResult(data: [Username: "user-name", Secret: "password"])
        1 * credsStoreHelper.getAuthentication("test-helper", "host2.name") >> new CredsStoreHelperResult(data: [Username: "<token>", Secret: "baz"])
        0 * credsStoreHelper._
        result.size() == 2
        result["host1.name"] == new AuthConfig(username: "user-name", password: "password", serveraddress: "host1.name")
        result["host2.name"] == new AuthConfig(identitytoken: "baz", serveraddress: "host2.name")
    }
}
