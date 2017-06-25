package de.gesellix.docker.client.system

import de.gesellix.docker.client.DockerAsyncCallback
import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.engine.EngineClient
import de.gesellix.docker.engine.EngineResponse
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ManageSystemClientTest extends Specification {
    EngineClient httpClient = Mock(EngineClient)
    ManageSystemClient service

    def setup() {
        service = new ManageSystemClient(httpClient, Mock(DockerResponseHandler))
    }

    def "system df"() {
        when:
        service.systemDf()

        then:
        1 * httpClient.get([path : "/system/df",
                            query: [:]])
    }

    def "ping"() {
        when:
        service.ping()

        then:
        1 * httpClient.get([path: "/_ping", timeout: 2000])
    }

    def "version"() {
        when:
        service.version()

        then:
        1 * httpClient.get([path: "/version"])
    }

    def "info"() {
        when:
        service.info()

        then:
        1 * httpClient.get([path: "/info"])
    }

    def "events (streaming)"() {
        given:
        def latch = new CountDownLatch(1)
        def content = new ByteArrayInputStream('{"status":"created"}\n'.bytes)
        DockerAsyncCallback callback = new DockerAsyncCallback() {
            def events = []

            @Override
            onEvent(Object event) {
                events << event
                latch.countDown()
            }

            @Override
            onFinish() {
            }
        }

        when:
        service.events(callback)
        latch.await(5000, TimeUnit.SECONDS)

        then:
        1 * httpClient.get([path: "/events", query: [:], async: true]) >> new EngineResponse(
                status: [success: true],
                stream: content)
        and:
        callback.events.first() == '{"status":"created"}'
    }

    def "events (polling)"() {
        given:
        def latch = new CountDownLatch(1)
        def content = new ByteArrayInputStream('{"status":"created"}\n'.bytes)
        DockerAsyncCallback callback = new DockerAsyncCallback() {
            def events = []

            @Override
            onEvent(Object event) {
                events << event
                latch.countDown()
            }

            @Override
            onFinish() {
            }
        }
        def since = new Date().time

        when:
        service.events(callback, [since: since])
        latch.await(5000, TimeUnit.SECONDS)

        then:
        1 * httpClient.get([path: "/events", query: [since: since], async: true]) >> new EngineResponse(
                status: [success: true],
                stream: content)
        and:
        callback.events.first() == '{"status":"created"}'
    }

    def "events (with filters)"() {
        when:
        service.events(Mock(DockerAsyncCallback), [filters: [container: ["foo"]]])

        then:
        1 * httpClient.get([path: "/events", query: ['filters': '{"container":["foo"]}'], async: true]) >> new EngineResponse(
                status: [success: true])
    }
}
