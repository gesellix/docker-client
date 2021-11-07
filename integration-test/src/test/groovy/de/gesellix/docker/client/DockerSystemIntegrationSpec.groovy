package de.gesellix.docker.client

import com.squareup.moshi.Moshi
import de.gesellix.docker.client.system.SystemEventsRequest
import de.gesellix.docker.remote.api.ContainerCreateRequest
import de.gesellix.docker.remote.api.EventMessage
import de.gesellix.docker.remote.api.core.Cancellable
import de.gesellix.docker.remote.api.core.StreamCallback
import groovy.util.logging.Slf4j
import spock.lang.Requires
import spock.lang.Specification

import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.CountDownLatch

import static de.gesellix.docker.client.TestConstants.CONSTANTS
import static java.util.concurrent.TimeUnit.SECONDS

@Slf4j
@Requires({ LocalDocker.available() })
class DockerSystemIntegrationSpec extends Specification {

  private Moshi moshi = new Moshi.Builder().build()

  static DockerClient dockerClient

  def setupSpec() {
    dockerClient = new DockerClientImpl()
  }

  def ping() {
    expect:
    "OK" == dockerClient.ping().content
  }

  def "events (async)"() {
    given:
    def latch = new CountDownLatch(1)
    Cancellable cancellableAction = null
    def callback = new StreamCallback<EventMessage>() {

      List<EventMessage> events = []

      @Override
      void onStarting(Cancellable cancellable) {
        cancellableAction = cancellable
      }

      @Override
      void onNext(EventMessage element) {
        log.info("[events (async)] $element")
        events << element
        latch.countDown()
      }
    }

    when:
    new Thread({
      dockerClient.events(new SystemEventsRequest(), callback, Duration.of(11, ChronoUnit.SECONDS))
    }).start()
    Thread.sleep(1000)
    String containerId = dockerClient.createContainer(new ContainerCreateRequest().tap { image = CONSTANTS.imageName }, "event-test-async").content.id
    latch.await(10, SECONDS)

    then:
    callback.events.size() == 1
    and:
    callback.events.first().action == "create"
    callback.events.first().actor.ID == containerId

    cleanup:
    cancellableAction?.cancel()
    dockerClient.rm(containerId)
  }

  def "events (poll)"() {
    // meh. boot2docker/docker-machine sometimes need a time update, e.g. via:
    // docker-machine ssh default 'sudo ntpclient -s -h pool.ntp.org'

    given:
    def dockerSystemTime = ZonedDateTime.parse(dockerClient.info().content.systemTime).toInstant()
    long dockerEpoch = (long) (dockerSystemTime.toEpochMilli() / 1000)

    def localSystemTime = Instant.now()
    long localEpoch = (long) (localSystemTime.toEpochMilli() / 1000)

    long timeOffset = localEpoch - dockerEpoch

    def latch = new CountDownLatch(1)
    Cancellable cancellableAction = null
    def callback = new StreamCallback<EventMessage>() {

      List<EventMessage> events = []

      @Override
      void onStarting(Cancellable cancellable) {
        cancellableAction = cancellable
      }

      @Override
      void onNext(EventMessage element) {
        log.info("[events (poll)] $element")
        events << element
        if (events.last().action == "destroy") {
          latch.countDown()
        }
      }
    }

    String container1 = dockerClient.createContainer(new ContainerCreateRequest().tap { image = CONSTANTS.imageName }, "c1").content.id
    log.debug("container1: ${container1}")

    Thread.sleep(1000)
    long epochBeforeRm = (long) ((Instant.now().toEpochMilli() / 1000) + timeOffset - 1000)
    dockerClient.rm(container1)

    when:
    String filters = moshi.adapter(Map).toJson([container: [container1]])
    new Thread({
      dockerClient.events(
          new SystemEventsRequest("${epochBeforeRm}", null, filters),
          callback, Duration.of(11, ChronoUnit.SECONDS))
    }).start()
    latch.await(10, SECONDS)

    then:
    !callback.events.empty
    and:
    def destroyEvents = new ArrayList<>(callback.events).findAll { it.action == "destroy" }
    destroyEvents.find { it.actor.ID == container1 }

    cleanup:
    cancellableAction?.cancel()
    dockerClient.rm(container1)
  }
}
