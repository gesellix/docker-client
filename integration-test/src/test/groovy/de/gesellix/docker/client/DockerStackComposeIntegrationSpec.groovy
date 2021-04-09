package de.gesellix.docker.client

import de.gesellix.docker.client.stack.DeployConfigReader
import de.gesellix.docker.client.stack.DeployStackOptions
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
    performSilently { dockerClient.leaveSwarm([force: true]) }
  }

  def cleanup() {
    Thread.sleep(1000)
  }

  def "deploy a new stack with compose file"() {
    given:
    def swarmConfig = dockerClient.newSwarmConfig()
    swarmConfig.AdvertiseAddr = swarmAdvertiseAddr
    dockerClient.initSwarm(swarmConfig)

    def composeStream = composeFilePath.toFile().newInputStream()
    def environment = [IMAGE_VERSION: '3.4', SOME_VAR: 'some-value']
    def namespace = "new-stack"
    def options = new DeployStackOptions()
    def workingDir = composeFilePath.parent.toString()
    def config = new DeployConfigReader(dockerClient).loadCompose(namespace, composeStream, workingDir, environment)

    when:
    dockerClient.stackDeploy(namespace, config, options)

    then:
    def tasks = delay { dockerClient.stackPs(namespace).content }
    tasks.size() == config.services.size()

    def spec = dockerClient.inspectService("${namespace}_service").content.Spec
    def containerSpec = spec.TaskTemplate.ContainerSpec

    spec.EndpointSpec == [Mode: 'vip']
    spec.Labels == ['com.docker.stack.namespace': namespace]
    spec.Mode == [Replicated: [Replicas: 1]]
    spec.Name == "${namespace}_service"
    spec.Networks.Aliases == [['service']]
    spec.TaskTemplate.RestartPolicy == [
        Condition  : 'on-failure',
        Delay      : 5000000000,
        MaxAttempts: 3,
        Window     : 120000000000
    ]

    containerSpec.Args == ['sh']
    containerSpec.Env == ['SOME_VAR=' + environment.SOME_VAR]
    containerSpec.Image =~ "alpine:${environment.IMAGE_VERSION}(@sha256:[a-f0-9]{64})?"
    containerSpec.Labels == ['com.docker.stack.namespace': namespace]
    containerSpec.Mounts == [
        [
            Type         : 'volume',
            Source       : namespace + '_shm',
            Target       : '/dev/shm',
            VolumeOptions: [Labels: ['com.docker.stack.namespace': namespace]]
        ]
    ]
    containerSpec.Configs.findAll { it.ConfigName == "${namespace}_my-config" }.size() == 1
    containerSpec.Secrets.findAll { it.SecretName == "${namespace}_my-secret" }.size() == 1

    def networkInfo = delayAndRetrySilently { dockerClient.inspectNetwork("${namespace}_my-subnet") }
    networkInfo.status.code == 200

    def volumeInfo = delayAndRetrySilently { dockerClient.inspectVolume("${namespace}_shm") }
    volumeInfo.status.code == 200

    cleanup:
    composeStream.close()
    performSilently { dockerClient.stackRm(namespace) }
    delayAndRetrySilently { dockerClient.rmVolume("${namespace}_shm") }
    performSilently { dockerClient.leaveSwarm([force: true]) }
  }

  def performSilently(Closure action) {
    try {
      action()
    }
    catch (Exception ignored) {
    }
  }

  def delay(Integer secondsToWait = 1, Closure action) {
    Thread.sleep(secondsToWait * 1000)
    action()
  }

  def delayAndRetrySilently(Integer secondsToWait = 1, Closure action, Integer retryCount = 5) {
    Object retVal = null
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
