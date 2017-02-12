package de.gesellix.docker.client.config

import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.testutil.ResourceReader
import spock.lang.Specification

class DockerEnvTest extends Specification {

    DockerEnv env

    def setup() {
        env = new DockerEnv()
    }

    def "read configured docker config.json"() {
        given:
        def expectedConfigDir = new File('.').absoluteFile
        def oldDockerConfigDir = System.setProperty("docker.config", expectedConfigDir.absolutePath)

        when:
        def dockerConfigFile = env.getDockerConfigFile()

        then:
        dockerConfigFile.absolutePath == new File(expectedConfigDir, 'config.json').absolutePath

        cleanup:
        if (oldDockerConfigDir) {
            System.setProperty("docker.config", oldDockerConfigDir)
        } else {
            System.clearProperty("docker.config")
        }
    }

    def "read default docker config file"() {
        given:
        def oldDockerConfig = System.clearProperty("docker.config")
        def expectedConfigFile = new ResourceReader().getClasspathResourceAsFile('/auth/config.json', DockerClient)
        env.configFile = expectedConfigFile

        when:
        def actualConfigFile = env.getDockerConfigFile()

        then:
        actualConfigFile == expectedConfigFile

        cleanup:
        if (oldDockerConfig) {
            System.setProperty("docker.config", oldDockerConfig)
        }
    }

    def "read legacy docker config file"() {
        given:
        def oldDockerConfig = System.clearProperty("docker.config")

        def nonExistingFile = new File('./I should not exist')
        assert !nonExistingFile.exists()
        env.configFile = nonExistingFile

        def expectedConfigFile = new ResourceReader().getClasspathResourceAsFile('/auth/dockercfg', DockerClient)
        env.legacyConfigFile = expectedConfigFile

        when:
        def actualConfigFile = env.getDockerConfigFile()

        then:
        actualConfigFile == expectedConfigFile

        cleanup:
        if (oldDockerConfig) {
            System.setProperty("docker.config", oldDockerConfig)
        }
    }
}
