package de.gesellix.docker.client.authentication

import spock.lang.Specification

import static de.gesellix.docker.client.authentication.AuthConfig.EMPTY_AUTH_CONFIG

class FileStoreTest extends Specification {

    def "getAuthConfig returns entry matching registry hostname"() {
        given:
        FileStore credsStore = new FileStore([auths: ["host.name": [auth: "Z2VzZWxsaXg6LXlldC1hbm90aGVyLXBhc3N3b3JkLQ=", email: "tobias@gesellix.de"]]])

        when:
        AuthConfig result = credsStore.getAuthConfig("host.name")

        then:
        result == new AuthConfig(
                username: "gesellix",
                password: "-yet-another-password-",
                email: "tobias@gesellix.de",
                serveraddress: "host.name"
        )
    }

    def "getAuthConfig returns empty AuthConfig for missing entry"() {
        given:
        FileStore credsStore = new FileStore([auths: ["host.name": [auth: "Z2VzZWxsaXg6LXlldC1hbm90aGVyLXBhc3N3b3JkLQ=", email: "tobias@gesellix.de"]]])

        expect:
        EMPTY_AUTH_CONFIG == credsStore.getAuthConfig("missing.host.name")
    }

    def "getAuthConfig returns empty AuthConfig for empty entry"() {
        given:
        FileStore credsStore = new FileStore([auths: ["host.name": [:]]])

        expect:
        EMPTY_AUTH_CONFIG == credsStore.getAuthConfig("host.name")
    }

    def "getAuthConfig returns empty AuthConfig for empty auth"() {
        given:
        FileStore credsStore = new FileStore([auths: ["host.name": [auth: null, email: "tobias@gesellix.de"]]])

        expect:
        EMPTY_AUTH_CONFIG == credsStore.getAuthConfig("host.name")
    }

    def "getAuthConfigs all known AuthConfigs"() {
        given:
        FileStore credsStore = new FileStore([auths: [
                "host1.name": [auth: "Z2VzZWxsaXg6LXlldC1hbm90aGVyLXBhc3N3b3JkLQ=", email: "tobias@gesellix.de"],
                "host2.name": [auth: "Z2VzZWxsaXg6LWEtcGFzc3dvcmQtZm9yLXF1YXkt", email: "tobias@gesellix.de"]]])

        when:
        Map<String, AuthConfig> result = credsStore.getAuthConfigs()

        then:
        result.size() == 2
        result["host1.name"] == new AuthConfig(
                username: "gesellix",
                password: "-yet-another-password-",
                email: "tobias@gesellix.de",
                serveraddress: "host1.name"
        )
        result["host2.name"] == new AuthConfig(
                username: "gesellix",
                password: "-a-password-for-quay-",
                email: "tobias@gesellix.de",
                serveraddress: "host2.name"
        )
    }
}
