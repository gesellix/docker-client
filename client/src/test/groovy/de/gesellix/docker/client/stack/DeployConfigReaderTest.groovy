package de.gesellix.docker.client.stack

import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.stack.types.StackConfig
import de.gesellix.docker.client.stack.types.StackNetwork
import de.gesellix.docker.client.stack.types.StackSecret
import de.gesellix.docker.compose.types.Command
import de.gesellix.docker.compose.types.DriverOpts
import de.gesellix.docker.compose.types.Environment
import de.gesellix.docker.compose.types.External
import de.gesellix.docker.compose.types.Healthcheck
import de.gesellix.docker.compose.types.Ipam
import de.gesellix.docker.compose.types.IpamConfig
import de.gesellix.docker.compose.types.Labels
import de.gesellix.docker.compose.types.Limits
import de.gesellix.docker.compose.types.PortConfig
import de.gesellix.docker.compose.types.PortConfigs
import de.gesellix.docker.compose.types.Reservations
import de.gesellix.docker.compose.types.Resources
import de.gesellix.docker.compose.types.ServiceConfig
import de.gesellix.docker.compose.types.ServiceNetwork
import de.gesellix.docker.compose.types.ServiceSecret
import de.gesellix.docker.compose.types.ServiceVolume
import de.gesellix.docker.compose.types.ServiceVolumeBind
import de.gesellix.docker.compose.types.ServiceVolumeVolume
import de.gesellix.docker.compose.types.StackVolume
import de.gesellix.docker.engine.EngineResponse
import spock.lang.Ignore
import spock.lang.Specification

import java.time.Duration
import java.time.temporal.ChronoUnit

import static de.gesellix.docker.client.stack.types.ResolutionMode.ResolutionModeDNSRR
import static de.gesellix.docker.client.stack.types.RestartPolicyCondition.RestartPolicyConditionAny
import static de.gesellix.docker.client.stack.types.RestartPolicyCondition.RestartPolicyConditionOnFailure
import static de.gesellix.docker.compose.types.MountPropagation.PropagationShared
import static de.gesellix.docker.compose.types.MountPropagation.PropagationSlave
import static de.gesellix.docker.compose.types.ServiceVolumeType.TypeBind
import static de.gesellix.docker.compose.types.ServiceVolumeType.TypeVolume

class DeployConfigReaderTest extends Specification {

    DeployConfigReader reader

    def setup() {
        reader = new DeployConfigReader(Mock(DockerClient))
    }

    def "converts stack configs"() {
        given:
        def config1 = new de.gesellix.docker.compose.types.StackConfig()
        def config1File = getClass().getResource('/configs/config1.txt').file
        def config1FileDirectory = new File(config1File).parent
        config1.file = 'config1.txt'

        when:
        def result = reader.configs(
                "name-space",
                ['config-1'  : config1,
                 'ext-config': new de.gesellix.docker.compose.types.StackConfig(external: new External(external: true))],
                config1FileDirectory
        )

        then:
        result == [
                'config-1': new StackConfig(
                        data: new FileInputStream(config1File).bytes,
                        name: 'name-space_config-1',
                        labels: ['com.docker.stack.namespace': 'name-space'])
        ]
    }

    def "prepares service configs"() {
        given:
        def serviceConfig = new ServiceConfig(
                source: 'config-1',
                target: 'config-target',
                uid: '',
                gid: '',
                mode: 0)

        when:
        def result = reader.prepareServiceConfigs("name.space", [
                [(serviceConfig.source): serviceConfig]
        ])

        then:
        result == [
                [File      : [Name: "config-target", UID: "0", GID: "0", Mode: 292],
                 ConfigID  : "<WILL_BE_PROVIDED_DURING_DEPLOY>",
                 ConfigName: "name.space_config-1"]
        ]
    }

    def "converts stack secrets"() {
        given:
        def secret1 = new de.gesellix.docker.compose.types.StackSecret()
        def secret1File = getClass().getResource('/secrets/secret1.txt').file
        def secret1FileDirectory = new File(secret1File).parent
        secret1.file = 'secret1.txt'

        when:
        def result = reader.secrets(
                "name-space",
                ['secret-1'  : secret1,
                 'ext-secret': new de.gesellix.docker.compose.types.StackSecret(external: new External(external: true))],
                secret1FileDirectory
        )

        then:
        result == [
                'secret-1': new StackSecret(
                        data: new FileInputStream(secret1File).bytes,
                        name: 'name-space_secret-1',
                        labels: ['com.docker.stack.namespace': 'name-space'])
        ]
    }

    def "prepares service secrets"() {
        given:
        def serviceSecret = new ServiceSecret(
                source: 'secret-1',
                target: 'secret-target',
                uid: '',
                gid: '',
                mode: 0)

        when:
        def result = reader.prepareServiceSecrets("name.space", [
                [(serviceSecret.source): serviceSecret]
        ])

        then:
        result == [
                [File      : [Name: "secret-target", UID: "0", GID: "0", Mode: 292],
                 SecretID  : "<WILL_BE_PROVIDED_DURING_DEPLOY>",
                 SecretName: "name.space_secret-1"]
        ]
    }

    def "converts networks"() {
        given:
        reader.dockerClient.version() >> new EngineResponse(content: [:])
        def normalNet = new de.gesellix.docker.compose.types.StackNetwork(
                driver: "overlay",
                driverOpts: new DriverOpts(["opt": "value"]),
                ipam: new Ipam(
                        driver: "driver",
                        config: [new IpamConfig(subnet: '10.0.0.0')]
                ),
                labels: new Labels(["something": "labeled"])
        )
        def outsideNet = new de.gesellix.docker.compose.types.StackNetwork(
                external: new External(
                        external: true,
                        name: "special"))
        def attachableNet = new de.gesellix.docker.compose.types.StackNetwork(
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
                labels: [(ManageStackClient.LabelNamespace): "name-space"]
        )
        networks["attachablenet"] == new StackNetwork(
                attachable: true,
                driver: "overlay",
                labels: [(ManageStackClient.LabelNamespace): "name-space"]
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
                        (ManageStackClient.LabelNamespace): "name-space",
                        something                         : "labeled"
                ]
        )
    }

    def "converts service endpoints"() {
        given:
        def ports = new PortConfigs(portConfigs: [
                new PortConfig("host",
                               53,
                               1053,
                               "udp"),
                new PortConfig(null,
                               8080,
                               80,
                               null
                )
        ])

        when:
        def endpoints = reader.serviceEndpoints('dnsrr', ports)

        then:
        endpoints == [
                mode : ResolutionModeDNSRR.value,
                ports: [
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
        "global"     | null     || [global: [:]]
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
        reader.getBindOptions(new ServiceVolumeBind(propagation: PropagationSlave.propagation)) == [propagation: PropagationSlave.propagation]
    }

    def "test getBindOptions with unknown mode"() {
        expect:
        reader.getBindOptions(new ServiceVolumeBind()) == null
    }

    def "test ConvertVolumeToMountAnonymousVolume"() {
        when:
        def mounts = reader.volumeToMount(
                "name-space",
                new ServiceVolume(type: TypeVolume.typeName, target: "/foo/bar"),
                [:])
        then:
        mounts == [
                type  : "volume",
                target: "/foo/bar"
        ]
    }

    // TODO move to docker-compose-v3 project
    @Ignore
    def "test ConvertVolumeToMountInvalidFormat"() {
        when:
        reader.volumeToMount(
                "name-space",
                new ServiceVolume(type: TypeVolume.typeName, target: volume),
                [:])
        then:
        def exc = thrown(IllegalArgumentException)
        exc.message == "invalid volume: $volume"
        where:
        volume << ["::", "::cc", ":bb:", "aa::", "aa::cc", "aa:bb:", " : : ", " : :cc", " :bb: ", "aa: : ", "aa: :cc", "aa:bb: "]
    }

    def "test ConvertVolumeToMountNamedVolume"() {
        def stackVolumes = ["normal": new StackVolume(
                driver: "glusterfs",
                driverOpts: new DriverOpts(["opt": "value"]),
                labels: new Labels(["something": "labeled"])
        )]

        when:
        def mount = reader.volumeToMount(
                "name-space",
                new ServiceVolume(type: TypeVolume.typeName, source: "normal", target: "/foo", readOnly: true),
                stackVolumes)

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
        def stackVolumes = ["outside": new StackVolume(
                external: new External(
                        external: true,
                        name: "special"
                )
        )]

        when:
        def mount = reader.volumeToMount(
                "name-space",
                new ServiceVolume(type: TypeVolume.typeName, source: "outside", target: "/foo"),
                stackVolumes)

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
        def stackVolumes = ["outside": new StackVolume(
                external: new External(
                        external: true,
                        name: "special"
                )
        )]

        when:
        def mount = reader.volumeToMount(
                "name-space",
                new ServiceVolume(type: TypeVolume.typeName, source: "outside", target: "/foo", volume: new ServiceVolumeVolume(true)),
                stackVolumes)

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
        def mount = reader.volumeToMount(
                "name-space",
                new ServiceVolume(type: TypeBind.typeName, source: "/bar", target: "/foo", readOnly: true, bind: new ServiceVolumeBind(propagation: PropagationShared.propagation)),
                [:])

        then:
        mount == [
                type       : "bind",
                source     : "/bar",
                target     : "/foo",
                readOnly   : true,
                bindOptions: [
                        propagation: PropagationShared.propagation
                ]
        ]
    }

    def "test ConvertVolumeToMountVolumeDoesNotExist"() {
        when:
        reader.volumeToMount(
                "name-space",
                new ServiceVolume(type: TypeVolume.typeName, source: "unknown", target: "/foo", readOnly: true),
                [:])
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
        policy == [condition: RestartPolicyConditionAny.value]
    }

    def "test ConvertRestartPolicyFromFailure"() {
        when:
        def policy = reader.restartPolicy("on-failure:4", null)
        then:
        policy == [condition  : RestartPolicyConditionOnFailure.value,
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
                timeout : Duration.of(30, ChronoUnit.SECONDS).toNanos(),
                interval: Duration.of(2, ChronoUnit.MILLIS).toNanos(),
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

    def "test ConvertServiceNetworksOnlyDefault"() {
        given:
        reader.dockerClient.version() >> new EngineResponse(content: [:])
        Map<String, de.gesellix.docker.compose.types.StackNetwork> networkConfigs = [:]
        when:
        def result = reader.convertServiceNetworks(
                null,
                networkConfigs,
                "name-space",
                "service")
        then:
        result == [
                [
                        target : "name-space_default",
                        aliases: ["service"]
                ]
        ]
    }

    def "test ConvertServiceNetworks"() {
        given:
        reader.dockerClient.version() >> new EngineResponse(content: [:])
        Map<String, de.gesellix.docker.compose.types.StackNetwork> networkConfigs = [
                "front": new de.gesellix.docker.compose.types.StackNetwork(
                        external: new External(
                                external: true,
                                name: "fronttier"
                        )),
                "back" : new de.gesellix.docker.compose.types.StackNetwork(),
        ]
        Map<String, ServiceNetwork> networks = [
                "front": new ServiceNetwork(aliases: ["something"]),
                "back" : new ServiceNetwork(aliases: ["other"]),
        ]
        when:
        def result = reader.convertServiceNetworks(
                networks,
                networkConfigs,
                "name-space",
                "service")
        then:
        result == [
                [
                        target : "fronttier",
                        aliases: ["something", "service"],
                ],
                [
                        target : "name-space_back",
                        aliases: ["other", "service"],
                ]
        ]
    }

    def "test ConvertServiceNetworksCustomDefault"() {
        given:
        reader.dockerClient.version() >> new EngineResponse(content: [:])
        Map<String, de.gesellix.docker.compose.types.StackNetwork> networkConfigs = [
                "default": new de.gesellix.docker.compose.types.StackNetwork(
                        external: new External(
                                external: true,
                                name: "custom"
                        ))
        ]
        Map<String, ServiceNetwork> networks = [:]
        when:
        def result = reader.convertServiceNetworks(
                networks,
                networkConfigs,
                "name-space",
                "service")
        then:
        result == [
                [
                        target : "custom",
                        aliases: ["service"],
                ]
        ]
    }

    def "test ConvertEnvironment"() {
        when:
        def env = new Environment([key1: "value1"])
        def resourcesPath = new File(getClass().getResource("/logback-test.xml").file).parent
        def result = reader.convertEnvironment(resourcesPath, ["env-files/env.properties"], env)
        then:
        result == [
                "MY_ENV=VALUE",
                "MY_OTHER_ENV=ANOTHER Value",
                "key1=value1"
        ]
    }
}
