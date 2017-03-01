package de.gesellix.docker.compose

import de.gesellix.docker.compose.types.Command
import de.gesellix.docker.compose.types.ComposeConfig
import de.gesellix.docker.compose.types.Config
import de.gesellix.docker.compose.types.Deploy
import de.gesellix.docker.compose.types.DriverOpts
import de.gesellix.docker.compose.types.Environment
import de.gesellix.docker.compose.types.External
import de.gesellix.docker.compose.types.ExtraHosts
import de.gesellix.docker.compose.types.Healthcheck
import de.gesellix.docker.compose.types.Ipam
import de.gesellix.docker.compose.types.Labels
import de.gesellix.docker.compose.types.Limits
import de.gesellix.docker.compose.types.Logging
import de.gesellix.docker.compose.types.Network
import de.gesellix.docker.compose.types.Placement
import de.gesellix.docker.compose.types.PortConfig
import de.gesellix.docker.compose.types.PortConfigs
import de.gesellix.docker.compose.types.Reservations
import de.gesellix.docker.compose.types.Resources
import de.gesellix.docker.compose.types.RestartPolicy
import de.gesellix.docker.compose.types.Secret
import de.gesellix.docker.compose.types.Service
import de.gesellix.docker.compose.types.ServiceNetwork
import de.gesellix.docker.compose.types.Ulimits
import de.gesellix.docker.compose.types.UpdateConfig
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

    def "can load expanded port formats"() {
        given:
        def sampleConfig = newSampleConfigPortFormats()
        InputStream composeFile = getClass().getResourceAsStream('portformats/sample.yaml')

        when:
        def result = reader.load(composeFile)

        then:
        result.services.web.ports == sampleConfig
    }

    def "can load a full example"() {
        given:
        def sampleConfig = newSampleConfigFull()
        InputStream composeFile = getClass().getResourceAsStream('full/sample.yaml')

        when:
        def result = reader.load(composeFile)

        then:
        result.version == sampleConfig.version
        result.volumes == sampleConfig.volumes
        result.networks == sampleConfig.networks
        result.services == sampleConfig.services
        result.secrets == sampleConfig.secrets
        result == sampleConfig
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

    def newSampleConfigPortFormats() {
        return new PortConfigs(portConfigs: [
                new PortConfig(
                        mode: "ingress",
                        target: 8080,
                        published: 80,
                        protocol: "tcp"
                ),
                new PortConfig(
                        mode: "ingress",
                        target: 8081,
                        published: 81,
                        protocol: "tcp"
                ),
                new PortConfig(
                        mode: "ingress",
                        target: 8082,
                        published: 82,
                        protocol: "tcp"
                ),
                new PortConfig(
                        mode: "ingress",
                        target: 8090,
                        published: 90,
                        protocol: "udp"
                ),
                new PortConfig(
                        mode: "ingress",
                        target: 8091,
                        published: 91,
                        protocol: "udp"
                ),
                new PortConfig(
                        mode: "ingress",
                        target: 8092,
                        published: 92,
                        protocol: "udp"
                ),
                new PortConfig(
                        mode: "ingress",
                        target: 8500,
                        published: 85,
                        protocol: "tcp"
                ),
                new PortConfig(
                        mode: "ingress",
                        target: 8600,
                        published: 0,
                        protocol: "tcp"
                ),
                new PortConfig(
                        target: 53,
                        published: 10053,
                        protocol: "udp"
                ),
                new PortConfig(
                        mode: "host",
                        target: 22,
                        published: 10022
                )
        ])
    }

    def newSampleConfigFull() {
//        workingDir, err := os.Getwd()
//        homeDir := os.Getenv("HOME")

        def fooService = new Service(
                capAdd: ["ALL"],
                capDrop: ["NET_ADMIN", "SYS_ADMIN"],
                cgroupParent: "m-executor-abcd",
                command: new Command(parts: ['bundle', 'exec', 'thin', '-p', '3000']),
                containerName: "my-web-container",
                dependsOn: [
                        'db',
                        'redis'
                ],
                deploy: new Deploy(
                        mode: "replicated",
                        replicas: 6,
                        labels: new Labels(entries: ["FOO": "BAR"]),
                        updateConfig: new UpdateConfig(
                                parallelism: 3,
                                delay: '10s',
                                failureAction: 'continue',
                                monitor: '60s',
                                maxFailureRatio: 0.3
                        ),
                        resources: new Resources(
                                limits: new Limits(
                                        nanoCpus: 0.001,
                                        memory: "50M"
                                ),
                                reservations: new Reservations(
                                        nanoCpus: 0.0001,
                                        memory: "20M"
                                ),
                        ),
                        restartPolicy: new RestartPolicy(
                                condition: "on_failure",
                                delay: '5s',
                                maxAttempts: 3,
                                window: '120s'
                        ),
                        placement: new Placement(constraints: ["node=foo"])
                ),
                devices: ['/dev/ttyUSB0:/dev/ttyUSB0'],
                dns: [
                        '8.8.8.8',
                        '9.9.9.9'
                ],
                dnsSearch: [
                        'dc1.example.com',
                        'dc2.example.com'],
                domainname: 'foo.com',
                entrypoint: ['/code/entrypoint.sh', '-p', '3000'],
                envFile: [
                        './example1.env',
                        './example2.env'
                ],
                environment: new Environment(
                        entries: [
                                'RACK_ENV'      : 'development',
                                'SHOW'          : 'true',
                                'SESSION_SECRET': '',
//                                'FOO'           : '1',
//                                'BAR'           : '2',
                                'BAZ'           : '3'
                        ]
                ),
                expose: [
                        '3000',
                        '8000'
                ],
                externalLinks: [
                        "redis_1",
                        "project_db_1:mysql",
                        "project_db_1:postgresql"
                ],
                extraHosts: new ExtraHosts(entries: [
                        "otherhost": "50.31.209.229",
                        "somehost" : "162.242.195.82"
                ]),
//                HealthCheck: &types.HealthCheckConfig{
//                    Test:     types.HealthCheckTest([]string{"CMD-SHELL", "echo \"hello world\""}),
//                },
                healthcheck: new Healthcheck(
                        interval: '10s',
                        retries: 5,
                        test: new Command(parts: ['echo "hello world"']),
                        timeout: '1s'
                ),
                hostname: 'foo',
                image: 'redis',
                ipc: 'host',
                labels: new Labels(entries: [
                        'com.example.description': 'Accounting webapp',
                        'com.example.number'     : '42',
                        'com.example.empty-label': ''
                ]),
                links: [
                        'db',
                        'db:database',
                        'redis'
                ],
                logging: new Logging(
                        driver: 'syslog',
                        options: [
                                'syslog-address': 'tcp://192.168.0.42:123'
                        ]
                ),
                networks: [
                        'some-network'       : new ServiceNetwork(
                                aliases: ['alias1', 'alias3']
                        ),
                        'other-network'      : new ServiceNetwork(
                                ipv4Address: '172.16.238.10',
                                ipv6Address: '2001:3984:3989::10'
                        ),
                        'other-other-network': null
                ],
                macAddress: '02:42:ac:11:65:43',
                networkMode: 'container:0cfeab0f748b9a743dc3da582046357c6ef497631c1a016d28d2bf9b4f899f7b',
                pid: 'host',
                ports: new PortConfigs(
                        portConfigs: [
                                new PortConfig(
                                        mode: 'ingress',
                                        target: 3000,
                                        published: 0,
                                        protocol: 'tcp'
                                ),
                                new PortConfig(
                                        mode: 'ingress',
                                        target: 3000,
                                        published: 0,
                                        protocol: 'tcp'
                                ),
                                new PortConfig(
                                        mode: 'ingress',
                                        target: 3001,
                                        published: 0,
                                        protocol: 'tcp'
                                ),
                                new PortConfig(
                                        mode: 'ingress',
                                        target: 3002,
                                        published: 0,
                                        protocol: 'tcp'
                                ),
                                new PortConfig(
                                        mode: 'ingress',
                                        target: 3003,
                                        published: 0,
                                        protocol: 'tcp'
                                ),
                                new PortConfig(
                                        mode: 'ingress',
                                        target: 3004,
                                        published: 0,
                                        protocol: 'tcp'
                                ),
                                new PortConfig(
                                        mode: 'ingress',
                                        target: 3005,
                                        published: 0,
                                        protocol: 'tcp'
                                ),
                                new PortConfig(
                                        mode: 'ingress',
                                        target: 8000,
                                        published: 8000,
                                        protocol: 'tcp'
                                ),
                                new PortConfig(
                                        mode: 'ingress',
                                        target: 8080,
                                        published: 9090,
                                        protocol: 'tcp'
                                ),
                                new PortConfig(
                                        mode: 'ingress',
                                        target: 8081,
                                        published: 9091,
                                        protocol: 'tcp'
                                ),
                                new PortConfig(
                                        mode: 'ingress',
                                        target: 22,
                                        published: 49100,
                                        protocol: 'tcp'
                                ),
                                new PortConfig(
                                        mode: 'ingress',
                                        target: 8001,
                                        published: 8001,
                                        protocol: 'tcp'
                                ),
                                new PortConfig(
                                        mode: 'ingress',
                                        target: 5000,
                                        published: 5000,
                                        protocol: 'tcp'
                                ),
                                new PortConfig(
                                        mode: 'ingress',
                                        target: 5001,
                                        published: 5001,
                                        protocol: 'tcp'
                                ),
                                new PortConfig(
                                        mode: 'ingress',
                                        target: 5002,
                                        published: 5002,
                                        protocol: 'tcp'
                                ),
                                new PortConfig(
                                        mode: 'ingress',
                                        target: 5003,
                                        published: 5003,
                                        protocol: 'tcp'
                                ),
                                new PortConfig(
                                        mode: 'ingress',
                                        target: 5004,
                                        published: 5004,
                                        protocol: 'tcp'
                                ),
                                new PortConfig(
                                        mode: 'ingress',
                                        target: 5005,
                                        published: 5005,
                                        protocol: 'tcp'
                                ),
                                new PortConfig(
                                        mode: 'ingress',
                                        target: 5006,
                                        published: 5006,
                                        protocol: 'tcp'
                                ),
                                new PortConfig(
                                        mode: 'ingress',
                                        target: 5007,
                                        published: 5007,
                                        protocol: 'tcp'
                                ),
                                new PortConfig(
                                        mode: 'ingress',
                                        target: 5008,
                                        published: 5008,
                                        protocol: 'tcp'
                                ),
                                new PortConfig(
                                        mode: 'ingress',
                                        target: 5009,
                                        published: 5009,
                                        protocol: 'tcp'
                                ),
                                new PortConfig(
                                        mode: 'ingress',
                                        target: 5010,
                                        published: 5010,
                                        protocol: 'tcp'
                                )
                        ]
                ),
                privileged: true,
                readOnly: true,
                restart: 'always',
                securityOpt: [
                        "label=level:s0:c100,c200",
                        "label=type:svirt_apache_t"
                ],
                stdinOpen: true,
                stopGracePeriod: '20s',
                stopSignal: 'SIGUSR1',
                tmpfs: ['/run', '/tmp'],
                tty: true,
//                Ulimits: map[string]*types.UlimitsConfig{
//                    "nproc": {
//                        Single: 65535,
//                    },
//                    "nofile": {
//                        Soft: 20000,
//                        Hard: 40000,
//                    },
//                },
                ulimits: new Ulimits(),
                user: 'someone',
//                Volumes: []string{
//                    "/var/lib/mysql",
//                    "/opt/data:/var/lib/mysql",
//                    fmt.Sprintf("%s:/code", workingDir),
//                    fmt.Sprintf("%s/static:/var/www/html", workingDir),
//                    fmt.Sprintf("%s/configs:/etc/configs/:ro", homeDir),
//                    "datavolume:/var/lib/mysql",
//                },
                volumes: [
                        '/var/lib/mysql',
                        '/opt/data:/var/lib/mysql',
                        '.:/code',
                        './static:/var/www/html',
                        '~/configs:/etc/configs/:ro',
                        'datavolume:/var/lib/mysql'
                ],
                workingDir: '/code'
        )

        def composeConfig = new ComposeConfig()

        composeConfig.version = "3"

        composeConfig.services = [
                "foo": fooService]

        composeConfig.networks = [
                "some-network"          : null,
                "other-network"         : new Network(
                        driver: "overlay",
                        driverOpts: new DriverOpts(
                                options: [
                                        foo: "bar",
                                        baz: "1"]
                        ),
                        ipam: new Ipam(
                                driver: "overlay",
                                config: [
                                        new Config(subnet: '172.16.238.0/24'),
                                        new Config(subnet: '2001:3984:3989::/64')
                                ]
                        )
                ),
                "external-network"      : new Network(
                        external: new External(
//                                name: 'external-network',
                                external: true)
                ),
                "other-external-network": new Network(
                        external: new External(
                                name: 'my-cool-network',
                                external: true)
                )]

        composeConfig.volumes = [
                "some-volume"          : null,
                "other-volume"         : new Volume(
                        driver: "flocker",
                        driverOpts: new DriverOpts(
                                options: [
                                        foo: "bar",
                                        baz: "1"]
                        )),
                "external-volume"      : new Volume(
                        external: new External(
//                                name: 'external-volume',
                                external: true)
                ),
                "other-external-volume": new Volume(
                        external: new External(
                                name: 'my-cool-volume',
                                external: true)
                )]

        composeConfig.secrets = null

        return composeConfig
    }
}
