package de.gesellix.docker.compose

import de.gesellix.docker.compose.types.ComposeConfig
import de.gesellix.docker.compose.types.Config
import de.gesellix.docker.compose.types.DriverOpts
import de.gesellix.docker.compose.types.Environment
import de.gesellix.docker.compose.types.External
import de.gesellix.docker.compose.types.Ipam
import de.gesellix.docker.compose.types.Labels
import de.gesellix.docker.compose.types.Network
import de.gesellix.docker.compose.types.Secret
import de.gesellix.docker.compose.types.Service
import de.gesellix.docker.compose.types.Volume
import groovy.json.JsonSlurper
import spock.lang.Ignore
import spock.lang.Specification

class ComposeFileReaderTest extends Specification {

    ComposeFileReader reader

    def setup() {
        reader = new ComposeFileReader()
    }

    def "can parse yaml"() {
        given:
        def sampleObject = new JsonSlurper().parse(getClass().getResourceAsStream('parse/sample.json'))
        InputStream composeFile = getClass().getResourceAsStream('parse/sample.yaml')

        when:
        def result = reader.loadYaml(composeFile)

        then:
        result == sampleObject
    }

    def "can load yaml into pojo"() {
        given:
        def sampleConfig = newSampleConfig()
        InputStream composeFile = getClass().getResourceAsStream('parse/sample.yaml')

        when:
        def result = reader.load(composeFile)

        then:
        result.services == sampleConfig.services
        result.networks == sampleConfig.networks
        result.volumes == sampleConfig.volumes
        result.secrets == sampleConfig.secrets
        result == sampleConfig
    }

    def "can load environments as dict and as list"() {
        given:
        def expectedEnv = new Environment(entries: [
                "FOO" : "1",
                "BAR" : "2",
                "BAZ" : "2.5",
                "QUUX": ""])

        InputStream composeFile = getClass().getResourceAsStream('environment/sample.yaml')

        when:
        def result = reader.load(composeFile)

        then:
        result.services['dict-env'].environment == expectedEnv
        result.services['list-env'].environment == expectedEnv
    }

    def "can load version 3.1"() {
        given:
        def sampleConfig = newSampleConfigVersion_3_1()
        InputStream composeFile = getClass().getResourceAsStream('version_3_1/sample.yaml')

        when:
        def result = reader.load(composeFile)

        then:
        result.services == sampleConfig.services
        result.secrets == sampleConfig.secrets
        result == sampleConfig
    }

    def "can load attachable network"() {
        given:
        def sampleConfig = newSampleConfigAttachableNetwork()
        InputStream composeFile = getClass().getResourceAsStream('attachable/sample.yaml')

        when:
        def result = reader.load(composeFile)

        then:
        result.networks.mynet1 == sampleConfig.mynet1
        result.networks.mynet2 == sampleConfig.mynet2
    }

    @Ignore
    def "can interpolate environment variables"() {
        given:
        def home = System.getenv('HOME')
        def expectedLabels = new Labels(entries: [
                "home1"      : home,
                "home2"      : home,
                "nonexistent": "",
                "default"    : "default"
        ])

        InputStream composeFile = getClass().getResourceAsStream('interpolation/sample.yaml')

        when:
        def result = reader.load(composeFile)

        then:
        result.services.test.labels == expectedLabels
        result.networks.test.driver == home
        result.volumes.test.driver == home
    }

    def newSampleConfig() {
        new ComposeConfig(
                version: "3",
                services: [
                        "foo": new Service(
                                image: "busybox",
                                environment: [:],
                                networks: ["with_me": null]
                        ),
                        "bar": new Service(
                                image: "busybox",
                                environment: new Environment(entries: ["FOO": "1"]),
                                networks: ["with_ipam": null]
                        )
                ],
                networks: [
                        "default"  : new Network(
                                driver: "bridge",
                                driverOpts: new DriverOpts(options: ["beep": "boop"])
                        ),
                        "with_ipam": new Network(
                                ipam: new Ipam(
                                        driver: "default",
                                        config: [new Config(subnet: "172.28.0.0/16")])

                        )
                ],
                volumes: [
                        "hello": new Volume(
                                driver: "default",
                                driverOpts: new DriverOpts(options: ["beep": "boop"])
                        )
                ]
        )
    }

    def newSampleConfigVersion_3_1() {
        new ComposeConfig(
                version: "3.1",
                services: [
                        "foo": new Service(
                                image: "busybox",
                                secrets: ["super"]
                        )
                ],
                secrets: [
                        super: new Secret(
                                external: new External(external: true)
                        )
                ]
        )
    }

    def newSampleConfigAttachableNetwork() {
        return [
                mynet1: new Network(driver: "overlay", attachable: true),
                mynet2: new Network(driver: "bridge", attachable: false)
        ]
    }
}
