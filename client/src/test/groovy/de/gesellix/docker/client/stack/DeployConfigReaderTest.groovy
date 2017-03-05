package de.gesellix.docker.client.stack

import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.stack.types.StackNetwork
import de.gesellix.docker.client.stack.types.StackSecret
import de.gesellix.docker.compose.types.Command
import de.gesellix.docker.compose.types.Config
import de.gesellix.docker.compose.types.DriverOpts
import de.gesellix.docker.compose.types.External
import de.gesellix.docker.compose.types.Healthcheck
import de.gesellix.docker.compose.types.Ipam
import de.gesellix.docker.compose.types.Labels
import de.gesellix.docker.compose.types.Limits
import de.gesellix.docker.compose.types.Network
import de.gesellix.docker.compose.types.PortConfig
import de.gesellix.docker.compose.types.PortConfigs
import de.gesellix.docker.compose.types.Reservations
import de.gesellix.docker.compose.types.Resources
import de.gesellix.docker.compose.types.Secret
import de.gesellix.docker.compose.types.Volume
import spock.lang.Specification

import java.time.Duration
import java.time.temporal.ChronoUnit

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

    def "test isReadOnly"() {
        expect:
        reader.isReadOnly(mode) == expectedResult
        where:
        mode                 | expectedResult
        ["foo", "bar", "ro"] | true
        ["ro"]               | true
        []                   | false
        ["foo", "rw"]        | false
        ["foo"]              | false
    }

    def "test isNoCopy"() {
        expect:
        reader.isNoCopy(mode) == expectedResult
        where:
        mode                     | expectedResult
        ["foo", "bar", "nocopy"] | true
        ["nocopy"]               | true
        []                       | false
        ["foo", "rw"]            | false
        ["foo"]                  | false
    }

    def "test getBindOptions with known mode"() {
        expect:
        reader.getBindOptions(["slave"]) == [propagation: DeployConfigReader.PropagationSlave]
    }

    def "test getBindOptions with unknown mode"() {
        expect:
        reader.getBindOptions(["ro"]) == null
    }

    def "test ConvertVolumeToMountAnonymousVolume"() {
        when:
        def mounts = reader.volumeToMount("name-space", "/foo/bar", [:])
        then:
        mounts == [
                type  : "volume",
                target: "/foo/bar"
        ]
    }

    def "test ConvertVolumeToMountInvalidFormat"() {
        when:
        reader.volumeToMount("name-space", volume, [:])
        then:
        def exc = thrown(IllegalArgumentException)
        exc.message == "invalid volume: $volume"
        where:
        volume << ["::", "::cc", ":bb:", "aa::", "aa::cc", "aa:bb:", " : : ", " : :cc", " :bb: ", "aa: : ", "aa: :cc", "aa:bb: "]
    }

    def "test ConvertVolumeToMountNamedVolume"() {
        def stackVolumes = ["normal": new Volume(
                driver: "glusterfs",
                driverOpts: new DriverOpts(
                        options: ["opt": "value"]
                ),
                labels: new Labels(entries: [
                        "something": "labeled"
                ])
        )]

        when:
        def mount = reader.volumeToMount("name-space", "normal:/foo:ro", stackVolumes)

        then:
        mount == [
                type         : "volume",
                source       : "name-space_normal",
                target       : "/foo",
                readOnly     : true,
                volumeOptions: [
                        noCopy      : false,
                        labels      : [
                                (ManageStackClient.LabelNamespace): "name-space",
                                "something"                       : "labeled"
                        ],
                        driverConfig: [
                                name   : "glusterfs",
                                options: [
                                        "opt": "value"
                                ]
                        ]
                ]
        ]
    }

    def "test ConvertVolumeToMountNamedVolumeExternal"() {
        def stackVolumes = ["outside": new Volume(
                external: new External(
                        external: true,
                        name: "special"
                )
        )]

        when:
        def mount = reader.volumeToMount("name-space", "outside:/foo", stackVolumes)

        then:
        mount == [
                type         : "volume",
                source       : "special",
                target       : "/foo",
                readOnly     : false,
                volumeOptions: [
                        noCopy: false
                ]
        ]
    }

    def "test ConvertVolumeToMountNamedVolumeExternalNoCopy"() {
        def stackVolumes = ["outside": new Volume(
                external: new External(
                        external: true,
                        name: "special"
                )
        )]

        when:
        def mount = reader.volumeToMount("name-space", "outside:/foo:nocopy", stackVolumes)

        then:
        mount == [
                type         : "volume",
                source       : "special",
                target       : "/foo",
                readOnly     : false,
                volumeOptions: [
                        noCopy: true
                ]
        ]
    }

    def "test ConvertVolumeToMountBind"() {
        when:
        def mount = reader.volumeToMount("name-space", "/bar:/foo:ro,shared", [:])

        then:
        mount == [
                type       : "bind",
                source     : "/bar",
                target     : "/foo",
                readOnly   : true,
                bindOptions: [
                        propagation: DeployConfigReader.PropagationShared
                ]
        ]
    }

    def "test ConvertVolumeToMountVolumeDoesNotExist"() {
        when:
        reader.volumeToMount("name-space", "unknown:/foo:ro", [:])
        then:
        def exc = thrown(IllegalArgumentException)
        exc.message == "undefined volume: unknown"
    }

    def "test ConvertResourcesFull"() {
        when:
        def result = reader.serviceResources(
                new Resources(
                        limits: new Limits(
                                nanoCpus: "0.003",
                                memory: "300000000"),
                        reservations: new Reservations(
                                nanoCpus: "0.002",
                                memory: "200000000")
                ))
        then:
        result == [
                limits      : [nanoCPUs   : 3000000,
                               memoryBytes: 300000000],
                reservations: [nanoCPUs   : 2000000,
                               memoryBytes: 200000000]]
    }

    def "test ConvertResourcesOnlyMemory"() {
        when:
        def result = reader.serviceResources(
                new Resources(
                        limits: new Limits(
                                memory: "300000000"),
                        reservations: new Reservations(
                                memory: "200000000")
                ))
        then:
        result == [
                limits      : [memoryBytes: 300000000],
                reservations: [memoryBytes: 200000000]]
    }

    def "test ConvertRestartPolicyFromNone"() {
        expect:
        null == reader.restartPolicy("no", null)
    }

    def "test ConvertRestartPolicyFromUnknown"() {
        when:
        reader.restartPolicy("unknown", null)
        then:
        def exc = thrown(IllegalArgumentException)
        exc.message == "unknown restart policy: unknown"
    }

    def "test ConvertRestartPolicyFromAlways"() {
        when:
        def policy = reader.restartPolicy("always", null)
        then:
        policy == [condition: DeployConfigReader.RestartPolicyCondition.RestartPolicyConditionAny]
    }

    def "test ConvertRestartPolicyFromFailure"() {
        when:
        def policy = reader.restartPolicy("on-failure:4", null)
        then:
        policy == [condition  : DeployConfigReader.RestartPolicyCondition.RestartPolicyConditionOnFailure,
                   maxAttempts: 4]
    }

    def "parse duration"() {
        expect:
        reader.parseDuration(input) == duration
        where:
        input   | duration
        "1.2ms" | Duration.of(1200, ChronoUnit.MICROS)
        "-1ns"  | Duration.of(-1, ChronoUnit.NANOS)
        ".2us"  | Duration.of(200, ChronoUnit.NANOS)
        "1.s"   | Duration.of(1, ChronoUnit.SECONDS)
        "2h3m"  | Duration.of(2, ChronoUnit.HOURS).plus(3, ChronoUnit.MINUTES)
    }

    def "test ConvertHealthcheck"() {
        expect:
        reader.convertHealthcheck(new Healthcheck(
                test: new Command(parts: ["EXEC", "touch", "/foo"]),
                timeout: "30s",
                interval: "2ms",
                retries: 10
        )) == [
                test    : ["EXEC", "touch", "/foo"],
                timeout : Duration.of(30, ChronoUnit.SECONDS),
                interval: Duration.of(2, ChronoUnit.MILLIS),
                retries : 10
        ]
    }

    def "test ConvertHealthcheckDisable"() {
        expect:
        reader.convertHealthcheck(new Healthcheck(
                disable: true
        )) == [
                test: ["NONE"]
        ]
    }

    def "test ConvertHealthcheckDisableWithTest"() {
        when:
        reader.convertHealthcheck(new Healthcheck(
                disable: true,
                test: new Command(parts: ["EXEC", "touch"])
        ))
        then:
        def exc = thrown(IllegalArgumentException)
        exc.message =~ "test and disable can't be set"
    }
}
