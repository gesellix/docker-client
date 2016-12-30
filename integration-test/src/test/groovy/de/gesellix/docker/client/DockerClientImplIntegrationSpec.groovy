package de.gesellix.docker.client

import groovy.util.logging.Slf4j
import spock.lang.Requires
import spock.lang.Specification

@Slf4j
@Requires({ LocalDocker.available() })
class DockerClientImplIntegrationSpec extends Specification {

    static DockerClient dockerClient
    static boolean nativeWindows = LocalDocker.isNativeWindows()

    def setupSpec() {
        dockerClient = new DockerClientImpl(
//                new DockerEnv(
//                        dockerHost: "http://192.168.99.100:2376",
//                        certPath: "/Users/${System.getProperty('user.name')}/.docker/machine/machines/default",
//                        apiVersion: "v1.23")
        )
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
        info.Containers >= 0
        if (nativeWindows) {
            info.DockerRootDir == "C:\\\\ProgramData\\\\Docker"
        } else {
            info.DockerRootDir =~ "(/mnt/sda1)?/var/lib/docker"
        }

        def expectedDriverStatusProperties
        if (nativeWindows) {
            expectedDriverStatusProperties = ["Windows"]
        } else {
            expectedDriverStatusProperties = ["Backing Filesystem", "Native Overlay Diff" , "Supports d_type"]
        }
        info.DriverStatus.findAll {
            it.first() in expectedDriverStatusProperties
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
        info.MemoryLimit != nativeWindows
        info.NoProxy == "" || info.NoProxy == "*.local, 169.254/16"
        info.OomKillDisable == !nativeWindows
        info.RegistryConfig.IndexConfigs['docker.io'] == [
                "Mirrors" : null,
                "Name"    : "docker.io",
                "Official": true,
                "Secure"  : true]
        info.RegistryConfig.InsecureRegistryCIDRs == ["127.0.0.0/8"]
        info.RegistryConfig.Mirrors == []
        info.SystemTime =~ "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2,}.(\\d{3,}Z)?"
    }

    def version() {
        when:
        def version = dockerClient.version().content

        then:
        version.ApiVersion == "1.25"
        version.Arch == "amd64"
        version.BuildTime == "2016-12-17T01:34:17.687787854+00:00"
        version.GitCommit == "88862e7"
        version.GoVersion == "go1.7.3"
        version.KernelVersion =~ "\\d.\\d{1,2}.\\d{1,2}(-\\w+)?"
        version.MinAPIVersion == "1.12"
        version.Os == nativeWindows ? "windows" : "linux"
        version.Version == "1.13.0-rc4"
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
