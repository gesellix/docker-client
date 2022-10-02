package de.gesellix.docker.client.stack

import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.EngineResponseContent
import de.gesellix.docker.client.stack.types.StackConfig
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
import de.gesellix.docker.compose.types.StackNetwork
import de.gesellix.docker.compose.types.StackVolume
import de.gesellix.docker.remote.api.EndpointPortConfig
import de.gesellix.docker.remote.api.EndpointSpec
import de.gesellix.docker.remote.api.HealthConfig
import de.gesellix.docker.remote.api.IPAM
import de.gesellix.docker.remote.api.IPAMConfig
import de.gesellix.docker.remote.api.Limit
import de.gesellix.docker.remote.api.Mount
import de.gesellix.docker.remote.api.MountBindOptions
import de.gesellix.docker.remote.api.MountVolumeOptions
import de.gesellix.docker.remote.api.MountVolumeOptionsDriverConfig
import de.gesellix.docker.remote.api.Network
import de.gesellix.docker.remote.api.NetworkAttachmentConfig
import de.gesellix.docker.remote.api.NetworkCreateRequest
import de.gesellix.docker.remote.api.ResourceObject
import de.gesellix.docker.remote.api.ServiceSpecMode
import de.gesellix.docker.remote.api.ServiceSpecModeReplicated
import de.gesellix.docker.remote.api.SystemVersion
import de.gesellix.docker.remote.api.TaskSpecContainerSpecConfigsInner
import de.gesellix.docker.remote.api.TaskSpecContainerSpecConfigsInnerFile
import de.gesellix.docker.remote.api.TaskSpecContainerSpecSecretsInner
import de.gesellix.docker.remote.api.TaskSpecContainerSpecSecretsInnerFile
import de.gesellix.docker.remote.api.TaskSpecResources
import de.gesellix.docker.remote.api.TaskSpecRestartPolicy
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.temporal.ChronoUnit

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
         'ext-config': new de.gesellix.docker.compose.types.StackConfig(null, new External(true, ""), null)],
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
        'config-1',
        'config-target',
        '',
        '',
        0)

    when:
    def result = reader.prepareServiceConfigs("name.space", [
        [(serviceConfig.source): serviceConfig]
    ])

    then:
    result == [
        new TaskSpecContainerSpecConfigsInner(
            new TaskSpecContainerSpecConfigsInnerFile("config-target", "0", "0", 292),
            null,
            "<WILL_BE_PROVIDED_DURING_DEPLOY>",
            "name.space_config-1"
        )
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
         'ext-secret': new de.gesellix.docker.compose.types.StackSecret(null, new External(true, ""), null)],
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
        'secret-1',
        'secret-target',
        '',
        '',
        0)

    when:
    def result = reader.prepareServiceSecrets("name.space", [
        [(serviceSecret.source): serviceSecret]
    ])

    then:
    result == [
        new TaskSpecContainerSpecSecretsInner(
            new TaskSpecContainerSpecSecretsInnerFile("secret-target", "0", "0", 292),
            "<WILL_BE_PROVIDED_DURING_DEPLOY>",
            "name.space_secret-1"
        )
    ]
  }

  def "converts networks"() {
    given:
    reader.dockerClient.version() >> new EngineResponseContent<SystemVersion>(new SystemVersion())
    def normalNet = new StackNetwork(
        driver: "overlay",
        driverOpts: new DriverOpts(["opt": "value"]),
        ipam: new Ipam(
            driver: "driver",
            config: [new IpamConfig(subnet: '10.0.0.0')]
        ),
        labels: new Labels(["something": "labeled"])
    )
    def outsideNet = new StackNetwork(
        external: new External(
            external: true,
            name: "special"))
    def attachableNet = new StackNetwork(
        driver: "overlay",
        attachable: true)

    when:
    Map<String, NetworkCreateRequest> networks
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
    1 * reader.dockerClient.inspectNetwork("special") >> new EngineResponseContent<Network>(new Network().tap { scope = "swarm" })

    externals == ["special"]
    networks.keySet().sort() == ["default", "normal", "attachablenet"].sort()
    networks["default"] == new NetworkCreateRequest(
        "default", true,
        "overlay",
        null, null, null, null, null, null,
        [(ManageStackClient.LabelNamespace): "name-space"])
    networks["attachablenet"] == new NetworkCreateRequest(
        "attachablenet", true, "overlay", false, true,
        null, null, null, [:], [(ManageStackClient.LabelNamespace): "name-space"])
    networks["normal"] == new NetworkCreateRequest(
        "normal", true, "overlay",
        false, false, null,
        new IPAM("driver",
                 [new IPAMConfig().tap { subnet = '10.0.0.0' }],
                 null),
        null, [opt: "value"],
        [
            (ManageStackClient.LabelNamespace): "name-space",
            something                         : "labeled"
        ]
    )
  }

  def "converts service endpoints"() {
    given:
    def ports = new PortConfigs([
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
    endpoints == new EndpointSpec(
        EndpointSpec.Mode.Dnsrr,
        [
            new EndpointPortConfig(
                null,
                EndpointPortConfig.Protocol.Udp,
                53,
                1053,
                EndpointPortConfig.PublishMode.Host
            ),
            new EndpointPortConfig(
                null,
                null,
                8080,
                80,
                null
            )
        ]
    )
  }

  @Unroll
  def "converts service deploy mode '#mode'"() {
    expect:
    reader.serviceMode(mode, replicas) == serviceMode
    where:
    mode         | replicas || serviceMode
    "global"     | null     || new ServiceSpecMode().tap { global = [:] }
    null         | null     || new ServiceSpecMode().tap { replicated = new ServiceSpecModeReplicated(1) }
    ""           | null     || new ServiceSpecMode().tap { replicated = new ServiceSpecModeReplicated(1) }
    "replicated" | null     || new ServiceSpecMode().tap { replicated = new ServiceSpecModeReplicated(1) }
    "replicated" | 42       || new ServiceSpecMode().tap { replicated = new ServiceSpecModeReplicated(42) }
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
    reader.getBindOptions(new ServiceVolumeBind(PropagationSlave.propagation)) == new MountBindOptions(MountBindOptions.Propagation.Slave, null)
  }

  def "test getBindOptions with unknown mode"() {
    expect:
    reader.getBindOptions(new ServiceVolumeBind()) == null
  }

  def "test ConvertVolumeToMountAnonymousVolume"() {
    when:
    def mounts = reader.volumeToMount(
        "name-space",
        new ServiceVolume(TypeVolume.typeName, "", "/foo/bar", false, "", null, null),
        [:])
    then:
    mounts == new Mount("/foo/bar", null, Mount.Type.Volume, null, null, null, null, null)
  }

  // TODO move to docker-compose-v3 project
  @Ignore
  def "test ConvertVolumeToMountInvalidFormat"() {
    when:
    reader.volumeToMount(
        "name-space",
        new ServiceVolume(TypeVolume.typeName, null, volume, false, null, null, null),
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
        new ServiceVolume(TypeVolume.typeName, "normal", "/foo", true, "", null, null),
        stackVolumes)

    then:
    mount == new Mount(
        "/foo",
        "name-space_normal",
        Mount.Type.Volume,
        true,
        null,
        null,
        new MountVolumeOptions(
            false,
            [
                (ManageStackClient.LabelNamespace): "name-space",
                "something"                       : "labeled"
            ],
            new MountVolumeOptionsDriverConfig(
                "glusterfs",
                [
                    "opt": "value"
                ]
            )
        ),
        null
    )
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
        new ServiceVolume(TypeVolume.typeName, "outside", "/foo", false, "", null, null),
        stackVolumes)

    then:
    mount == new Mount(
        "/foo",
        "special",
        Mount.Type.Volume,
        false,
        null,
        null,
        new MountVolumeOptions(
            false,
            null,
            null
        ),
        null
    )
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
        new ServiceVolume(TypeVolume.typeName, "outside", "/foo", false, "", null, new ServiceVolumeVolume(true)),
        stackVolumes)

    then:
    mount == new Mount(
        "/foo",
        "special",
        Mount.Type.Volume,
        false,
        null,
        null,
        new MountVolumeOptions(
            true,
            null,
            null
        ),
        null
    )
  }

  def "test ConvertVolumeToMountBind"() {
    when:
    def mount = reader.volumeToMount(
        "name-space",
        new ServiceVolume(TypeBind.typeName, "/bar", "/foo", true, "", new ServiceVolumeBind(PropagationShared.propagation), null),
        [:])

    then:
    mount == new Mount(
        "/foo",
        "/bar",
        Mount.Type.Bind,
        true,
        null,
        new MountBindOptions(
            MountBindOptions.Propagation.Shared,
            null
        ),
        null,
        null
    )
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
    result == new TaskSpecResources(
        new Limit(3000000, 300000000, null),
        new ResourceObject(2000000, 200000000, null)
    )
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
    result == new TaskSpecResources(
        new Limit(null, 300000000, null),
        new ResourceObject(null, 200000000, null)
    )
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
    policy.condition == TaskSpecRestartPolicy.Condition.Any
  }

  def "test ConvertRestartPolicyFromFailure"() {
    when:
    def policy = reader.restartPolicy("on-failure:4", null)
    then:
    policy.condition == TaskSpecRestartPolicy.Condition.OnMinusFailure
    policy.maxAttempts == 4
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
    )) == new HealthConfig(
        ["EXEC", "touch", "/foo"],
        Duration.of(2, ChronoUnit.MILLIS).toNanos().longValue(),
        Duration.of(30, ChronoUnit.SECONDS).toNanos().longValue(),
        10,
        null
    )
  }

  def "test ConvertHealthcheckDisable"() {
    expect:
    reader.convertHealthcheck(new Healthcheck(
        disable: true
    )) == new HealthConfig(
        ["NONE"],
        null,
        null,
        null,
        null
    )
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
    reader.dockerClient.version() >> new EngineResponseContent(new SystemVersion())
    Map<String, StackNetwork> networkConfigs = [:]
    when:
    def result = reader.convertServiceNetworks(
        null,
        networkConfigs,
        "name-space",
        "service")
    then:
    result == [
        new NetworkAttachmentConfig("name-space_default", ["service"], null)
    ]
  }

  def "test ConvertServiceNetworks"() {
    given:
    reader.dockerClient.version() >> new EngineResponseContent(new SystemVersion())
    Map<String, StackNetwork> networkConfigs = [
        "front": new StackNetwork(
            external: new External(
                external: true,
                name: "fronttier"
            )),
        "back" : new StackNetwork(),
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
        new NetworkAttachmentConfig("fronttier", ["something", "service"], null),
        new NetworkAttachmentConfig("name-space_back", ["other", "service"], null)
    ]
  }

  def "test ConvertServiceNetworksCustomDefault"() {
    given:
    reader.dockerClient.version() >> new EngineResponseContent(new SystemVersion())
    Map<String, StackNetwork> networkConfigs = [
        "default": new StackNetwork(
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
        new NetworkAttachmentConfig("custom", ["service"], null)
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
