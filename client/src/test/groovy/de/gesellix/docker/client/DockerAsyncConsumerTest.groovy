package de.gesellix.docker.client

import de.gesellix.docker.engine.EngineResponse
import de.gesellix.docker.testutil.ChunkedStreamServer
import de.gesellix.docker.testutil.HttpTestServer
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import spock.lang.Specification

import static java.util.concurrent.Executors.newSingleThreadExecutor
import static java.util.concurrent.TimeUnit.SECONDS

@Slf4j
class DockerAsyncConsumerTest extends Specification {

  def "can read chunked json"() {
    given:
    def chunk1 = JsonOutput.toJson([
        status  : "create",
        id      : "event-1",
        Type    : "container",
        Action  : "destroy",
        Actor   : [ID        : "48574f47f045900c401e6eb86a138e865e70728965a76c67fea344405ae6953b",
                   Attributes: [name: "event-test-async"]],
        scope   : "local",
        time    : 1576102058,
        timeNano: 1576102058810155504
    ])
    def chunk2 = JsonOutput.toJson([
        status  : "destroy",
        id      : "event-2",
        Type    : "container",
        Action  : "destroy",
        Actor   : [ID        : "48574f47f045900c401e6eb86a138e865e70728965a76c67fea344405ae6953b",
                   Attributes: [name: "event-test-async"]],
        scope   : "local",
        time    : 1576102058,
        timeNano: 1576102058810155504
    ])
    def chunks = ['{"foo":"bar"}\n', chunk1, "\n", chunk2 + chunk2]

    def server = new HttpTestServer()
    def serverAddress = server.start('/events/', new ChunkedStreamServer(chunks, new Timeout(2, SECONDS)))
    def port = serverAddress.port

    def response = new EngineResponse(stream: URI.create("http://localhost:$port/events/").toURL().newInputStream())
    if (response.stream == null) {
      throw new IllegalStateException("stream should not be null")
    }

    def events = []
    def callback = new DockerAsyncCallback() {

      @Override
      def onEvent(Object event) {
        log.info("onEvent: $event")
        events << new JsonSlurper().parseText(event as String)
      }

      @Override
      def onFinish() {
        log.info("onFinish")
      }
    }

    def executor = newSingleThreadExecutor()
    def future = executor.submit(new DockerAsyncConsumer(response, callback))

    when:
    future.get(16, SECONDS)

    then:
    events.size() == 3
  }
}
