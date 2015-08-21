package de.gesellix.docker.client

import spock.lang.Specification

class EnvFileParserTest extends Specification {

    def "reads empty env file"() {
        when:
        def env = new EnvFileParser().parse(new ResourceReader().getClasspathResourceAsFile('/env-files/empty.properties'))
        then:
        env == []
    }

    def "reads env variables"() {
        when:
        def env = new EnvFileParser().parse(new ResourceReader().getClasspathResourceAsFile('/env-files/env.properties'))
        then:
        env == ["MY_ENV=VALUE", "MY_OTHER_ENV=ANOTHER Value"]
    }

    def "ignores empty lines"() {
        when:
        def env = new EnvFileParser().parse(new ResourceReader().getClasspathResourceAsFile('/env-files/env-with-empty-lines.properties'))
        then:
        env == ["MY_ENV=VALUE", "MY_OTHER_ENV=ANOTHER Value"]
    }

    def "ignores comments"() {
        when:
        def env = new EnvFileParser().parse(new ResourceReader().getClasspathResourceAsFile('/env-files/env-with-comments.properties'))
        then:
        env == ["MY_ENV=VALUE", "MY_OTHER_ENV=ANOTHER Value"]
    }

    def "trims leading whitespace on variable names"() {
        when:
        def env = new EnvFileParser().parse(new ResourceReader().getClasspathResourceAsFile('/env-files/env-with-leading-whitespace-variables.properties'))
        then:
        env == ["MY_ENV=VALUE", "A_VARIABLE= A VALUE"]
    }

    def "allows pass-through of environment variables"() {
        given:
        // this needs to set up in the current environment (see build.gradle)
        assert System.env["A_KNOWN_VARIABLE"] == "my value"
        when:
        def env = new EnvFileParser().parse(new ResourceReader().getClasspathResourceAsFile('/env-files/env-pass-through.properties'))
        then:
        env == ["MY_ENV=VALUE", "MY_OTHER_ENV=ANOTHER Value", "A_KNOWN_VARIABLE=my value"]
    }
}
