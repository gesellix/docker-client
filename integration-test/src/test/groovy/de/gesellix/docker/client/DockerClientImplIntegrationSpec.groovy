package de.gesellix.docker.client

import groovy.util.logging.Slf4j
import spock.lang.Requires
import spock.lang.Specification

import static de.gesellix.docker.client.TestConstants.CONSTANTS

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
            expectedDriverStatusProperties = ["Backing Filesystem"]
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

        def officialRegistry = info.RegistryConfig.IndexConfigs['docker.io']
        officialRegistry.Name == "docker.io"
        officialRegistry.Official == true
        officialRegistry.Secure == true

        info.RegistryConfig.InsecureRegistryCIDRs == ["127.0.0.0/8"]
        info.SystemTime =~ "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2,}.(\\d{3,}Z)?"
    }

    def version() {
        when:
        def version = dockerClient.version().content

        then:
        def missingKeys = CONSTANTS.versionDetails.findResults { key, matcher ->
            !version[key] ? (key) : null
        }
        missingKeys.empty
    }

    def auth() {
        given:
//        def authDetails = dockerClient.readAuthConfig(null, null)
        def authDetails = dockerClient.readDefaultAuthConfig()
        def authPlain = authDetails

        when:
        def authResult
        if (authDetails.username == null) {
            log.warn("no username configured to auth")
        } else {
            authResult = dockerClient.auth(authPlain)
        }

        then:
        authDetails.username == null || authResult.status.code == 200
    }
}
