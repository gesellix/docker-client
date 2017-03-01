package de.gesellix.docker.client.stack

import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.stack.types.StackNetwork
import de.gesellix.docker.client.stack.types.StackSecret
import de.gesellix.docker.compose.types.Config
import de.gesellix.docker.compose.types.DriverOpts
import de.gesellix.docker.compose.types.External
import de.gesellix.docker.compose.types.Ipam
import de.gesellix.docker.compose.types.Labels
import de.gesellix.docker.compose.types.Network
import de.gesellix.docker.compose.types.PortConfig
import de.gesellix.docker.compose.types.PortConfigs
import de.gesellix.docker.compose.types.Secret
import spock.lang.Specification

class DeployConfigReaderTest extends Specification {

    DeployConfigReader reader

    def setup() {
        reader = new DeployConfigReader(Mock(DockerClient))
    }

    def "converts secrets"() {
        given:
        def secret1 = new Secret()
        def secret1File = getClass().getResource('/secrets/secret1.txt').file
        secret1.file = secret1File

        when:
        def result = reader.secrets("name-space", [
                'secret-1'  : secret1,
                'ext-secret': new Secret(external: new External(external: true))])

        then:
        result == [
                'secret-1': new StackSecret(
                        data: new FileInputStream(secret1File).bytes,
                        name: 'name-space_secret-1',
                        labels: ['com.docker.stack.namespace': 'name-space'])
        ]
    }

    def "converts networks"() {
        given:
        def normalNet = new Network(
                driver: "overlay",
                driverOpts: new DriverOpts(options: [opt: "value"]),
                ipam: new Ipam(
                        driver: "driver",
                        config: [new Config(subnet: '10.0.0.0')]
                ),
                labels: new Labels(entries: ["something": "labeled"])
        )
        def outsideNet = new Network(
                external: new External(
                        external: true,
                        name: "special"))
        def attachableNet = new Network(
                driver: "overlay",
                attachable: true)

        when:
        Map<String, StackNetwork> networks
        List<String> externals

        (networks, externals) = reader.networks(
                "name-space",
                [
                        'normal',
                        'outside',
                        'default',
                        'attachablenet'
                ],
                [
                        'normal'       : normalNet,
                        'outside'      : outsideNet,
                        'attachablenet': attachableNet
                ]
        )

        then:
        1 * reader.dockerClient.inspectNetwork("special") >> [content: [Scope: "swarm"]]

        externals == ["special"]
        networks.keySet().sort() == ["default", "normal", "attachablenet"].sort()
        networks["default"] == new StackNetwork(
                driver: "overlay",
                labels: ["${ManageStackClient.LabelNamespace}=name-space" as String]
        )
        networks["attachablenet"] == new StackNetwork(
                attachable: true,
                driver: "overlay",
                labels: ["${ManageStackClient.LabelNamespace}=name-space" as String]
        )
        networks["normal"] == new StackNetwork(
                driver: "overlay",
                driverOpts: [
                        opt: "value"
                ],
                ipam: [
                        driver: "driver",
                        config: [
                                [subnet: '10.0.0.0']
                        ]
                ],
                labels: [
                        "${ManageStackClient.LabelNamespace}=name-space" as String,
                        "something=labeled"
                ]
        )
    }

    def "converts service endpoints"() {
        given:
        def ports = new PortConfigs(portConfigs: [
                new PortConfig(
                        protocol: "udp",
                        target: 53,
                        published: 1053,
                        mode: "host"),
                new PortConfig(
                        target: 8080,
                        published: 80
                )
        ])

        when:
        def endpoints = reader.serviceEndpoints(ports)

        then:
        endpoints == [ports: [
                [
                        protocol     : "udp",
                        targetPort   : 53,
                        publishedPort: 1053,
                        publishMode  : "host"
                ],
                [
                        protocol     : null,
                        targetPort   : 8080,
                        publishedPort: 80,
                        publishMode  : null
                ]
        ]
        ]
    }

    def "converts service deploy mode"() {
        expect:
        reader.serviceMode(mode, replicas) == serviceMode
        where:
        mode         | replicas || serviceMode
        "global"     | null     || [global: true]
        null         | null     || [replicated: [replicas: 1]]
        ""           | null     || [replicated: [replicas: 1]]
        "replicated" | null     || [replicated: [replicas: 1]]
        "replicated" | 42       || [replicated: [replicas: 42]]
    }
}
