package de.gesellix.docker.client

import de.gesellix.docker.client.stack.DeployConfigReader
import de.gesellix.docker.client.stack.DeployStackOptions
import de.gesellix.docker.remote.api.EndpointSpec
import de.gesellix.docker.remote.api.Mount
import de.gesellix.docker.remote.api.MountVolumeOptions
import de.gesellix.docker.remote.api.SwarmInitRequest
import de.gesellix.docker.remote.api.TaskSpecRestartPolicy
import de.gesellix.docker.testutil.SwarmUtil
import groovy.util.logging.Slf4j
import spock.lang.Requires
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths

@Slf4j
@Requires({ LocalDocker.available() && LocalDocker.supportsStack() })
class DockerStackComposeIntegrationSpec extends Specification {

  static DockerClient dockerClient
  static Path composeFilePath
  static String swarmAdvertiseAddr

  def setupSpec() {
    dockerClient = new DockerClientImpl()
    composeFilePath = Paths.get(getClass().getResource('compose/docker-stack.yml').toURI())
    swarmAdvertiseAddr = new SwarmUtil().getAdvertiseAddr()
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def cleanup() {
    Thread.sleep(1000)
  }

  // Service currently fails with:
  // "failed during hnsCallRawResponse: hnsCall failed in Win32: The parameter is incorrect. (0x57)"
  // This one might be related to https://github.com/moby/moby/issues/40621
  @Requires({ !LocalDocker.isNativeWindows() })
  def "deploy a new stack with compose file"() {
    given:
    def swarmConfig = dockerClient.newSwarmInitRequest()
    dockerClient.initSwarm(new SwarmInitRequest(swarmConfig.listenAddr, swarmAdvertiseAddr, null, null, null, null, null, null))

    def composeStream = composeFilePath.toFile().newInputStream()
    def environment = [
        IMAGE_VERSION: TestConstants.CONSTANTS.imageTag,
        VOLUME_TARGET: TestConstants.CONSTANTS.volumeTarget,
        SOME_VAR     : 'some-value']
    def namespace = "new-stack"
    def options = new DeployStackOptions(sendRegistryAuth: true)
    def workingDir = composeFilePath.parent.toString()
    def config = new DeployConfigReader(dockerClient).loadCompose(namespace, composeStream, workingDir, environment)

    when:
    dockerClient.stackDeploy(namespace, config, options)

    then:
    def tasks = delay { dockerClient.stackPs(namespace).content }
    tasks.size() == config.services.size()

    def spec = dockerClient.inspectService("${namespace}_service").content.spec
    def containerSpec = spec.taskTemplate.containerSpec

    spec.endpointSpec.mode == EndpointSpec.Mode.Vip
    spec.labels == ['com.docker.stack.namespace': namespace]
    spec.mode.replicated.replicas == 1
    spec.name == "${namespace}_service"
    spec.networks.aliases == [['service']]
    spec.taskTemplate.restartPolicy.condition == TaskSpecRestartPolicy.Condition.OnMinusFailure
    spec.taskTemplate.restartPolicy.delay == 5000000000
    spec.taskTemplate.restartPolicy.maxAttempts == 3
    spec.taskTemplate.restartPolicy.window == 120000000000

    containerSpec.args == ['-']
    containerSpec.env == ['SOME_VAR=' + environment.SOME_VAR]
    containerSpec.image =~ "gesellix/echo-server:${environment.IMAGE_VERSION}(@sha256:[a-f0-9]{64})?"
    containerSpec.labels == ['com.docker.stack.namespace': namespace]
    containerSpec.mounts == [
        new Mount(
            TestConstants.CONSTANTS.volumeTarget,
            "${namespace}_example",
            Mount.Type.Volume,
            null, null, null,
            new MountVolumeOptions(
                false,
                ['com.docker.stack.namespace': namespace],
                null
            ),
            null
        )
    ]
    containerSpec.configs.findAll { it.configName == "${namespace}_my-config" }.size() == 1
    containerSpec.secrets.findAll { it.secretName == "${namespace}_my-secret" }.size() == 1

    def networkInfo = delayAndRetrySilently { dockerClient.inspectNetwork("${namespace}_my-subnet") }
    networkInfo.content.name == "${namespace}_my-subnet"

    def volumeInfo = delayAndRetrySilently { dockerClient.inspectVolume("${namespace}_example") }
    println "volumes: ${dockerClient.volumes().content}"
    volumeInfo.content.name == "${namespace}_example"

    cleanup:
    composeStream.close()
    performSilently { dockerClient.stackRm(namespace) }
    delayAndRetrySilently { dockerClient.rmVolume("${namespace}_example") }
    performSilently { dockerClient.leaveSwarm(true) }
  }

  def performSilently(Closure action) {
    try {
      action()
    }
    catch (Exception ignored) {
    }
  }

  def <R> R delay(Integer secondsToWait = 1, Closure<R> action) {
    Thread.sleep(secondsToWait * 1000)
    action()
  }

  def <R> R delayAndRetrySilently(Integer secondsToWait = 1, Closure<R> action, Integer retryCount = 5) {
    R retVal = null
    for (_ in 1..retryCount) {
      try {
        Thread.sleep(secondsToWait * 1000)
        retVal = action()
        break
      }
      catch (Exception ignored) {}
    }

    retVal
  }
}
