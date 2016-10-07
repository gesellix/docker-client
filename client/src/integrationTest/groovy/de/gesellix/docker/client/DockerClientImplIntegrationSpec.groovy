package de.gesellix.docker.client

import de.gesellix.docker.registry.DockerRegistry
import groovy.util.logging.Slf4j
import spock.lang.Requires
import spock.lang.Specification

@Slf4j
@Requires({ LocalDocker.available() })
class DockerClientImplIntegrationSpec extends Specification {

    static DockerRegistry registry

    static DockerClient dockerClient

    def setupSpec() {
        dockerClient = new DockerClientImpl(
//                new DockerEnv(
//                        dockerHost: "http://192.168.99.100:2376",
//                        certPath: "/Users/${System.getProperty('user.name')}/.docker/machine/machines/default",
//                        apiVersion: "v1.23")
        )
        registry = new DockerRegistry(dockerClient)
        registry.run()
    }

    def cleanupSpec() {
        registry.rm()
    }

    def ping() {
        when:
        def ping = dockerClient.ping()

        then:
        ping.status.code == 200
        ping.content == "OK"
    }

    def info() {
        when:
        def info = dockerClient.info().content

        then:
        def expectedKeys = [
                "Architecture",
                "BridgeNfIp6tables", "BridgeNfIptables",
                "CPUSet", "CPUShares", "CgroupDriver", "ClusterAdvertise", "ClusterStore", "Containers", "ContainersPaused", "ContainersRunning", "ContainersStopped", "CpuCfsPeriod", "CpuCfsQuota",
                "Debug", "DockerRootDir", "Driver", "DriverStatus",
                "ExecutionDriver", "ExperimentalBuild",
                "HttpProxy", "HttpsProxy",
                "ID", "IPv4Forwarding", "Images", "IndexServerAddress",
                "KernelMemory", "KernelVersion",
                "Labels", "LoggingDriver",
                "MemTotal", "MemoryLimit",
                "NCPU", "NEventsListener", "NFd", "NGoroutines", "Name", "NoProxy",
                "OSType", "OomKillDisable", "OperatingSystem",
                "Plugins",
                "RegistryConfig",
                "ServerVersion", "SwapLimit", "SystemStatus", "SystemTime"]
        new ArrayList<>(info.keySet() as Set).each { expectedKeys.contains(it) }

        and:
        def expectedDriverStatusProperties = ["Root Dir", "Backing Filesystem", "Dirs", "Dirperm1 Supported"]
        info.Containers >= 0
        info.DockerRootDir =~ "(/mnt/sda1)?/var/lib/docker"
        info.DriverStatus.findAll {
            def propertyName = it.first()
            propertyName in expectedDriverStatusProperties
        }.size() == expectedDriverStatusProperties.size()
        info.HttpProxy == ""
        info.HttpsProxy == ""
        info.ID =~ "([0-9A-Z]{4}:?){12}"
        info.Images > 0
        info.IndexServerAddress == "https://index.docker.io/v1/"
        info.IPv4Forwarding == true
        info.Labels == null
        info.LoggingDriver == "json-file"
        info.MemTotal > 0
        info.MemoryLimit == true
        info.NoProxy == "" || info.NoProxy == "*.local, 169.254/16"
        info.OomKillDisable == true
        info.RegistryConfig.IndexConfigs['docker.io'] == [
                "Mirrors" : null,
                "Name"    : "docker.io",
                "Official": true,
                "Secure"  : true]
        info.RegistryConfig.InsecureRegistryCIDRs == ["127.0.0.0/8"]
        info.RegistryConfig.Mirrors == null
        info.SystemTime =~ "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2,}.(\\d{3,}Z)?"
    }

    def version() {
        when:
        def version = dockerClient.version().content

        then:
        version.ApiVersion == "1.24"
        version.Arch == "amd64"
        version.BuildTime == "2016-09-27T23:38:15.810178467+00:00"
        version.GitCommit == "45bed2c"
        version.GoVersion == "go1.6.3"
        version.KernelVersion =~ "\\d.\\d{1,2}.\\d{1,2}(-\\w+)?"
        version.Os == "linux"
        version.Version == "1.12.2-rc1"
    }

    def auth() {
        given:
        def authDetails = dockerClient.readAuthConfig(null, null)
        def authPlain = authDetails

        when:
        def authResult = dockerClient.auth(authPlain)

        then:
        authResult.status.code == 200
    }
}
