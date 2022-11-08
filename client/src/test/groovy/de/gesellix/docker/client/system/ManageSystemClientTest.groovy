package de.gesellix.docker.client.system

import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.SystemDataUsageResponse
import de.gesellix.docker.remote.api.SystemInfo
import de.gesellix.docker.remote.api.SystemVersion
import de.gesellix.docker.remote.api.client.SystemApi
import de.gesellix.docker.remote.api.core.StreamCallback
import spock.lang.Specification

import java.time.Duration
import java.time.temporal.ChronoUnit

class ManageSystemClientTest extends Specification {

  EngineApiClient client = Mock(EngineApiClient)
  ManageSystemClient service

  def setup() {
    service = new ManageSystemClient(client)
  }

  def "data usage"() {
    given:
    def systemApi = Mock(SystemApi)
    client.systemApi >> systemApi
    def dataUsageResponse = Mock(SystemDataUsageResponse)

    when:
    def systemDf = service.systemDf()

    then:
    1 * systemApi.systemDataUsage() >> dataUsageResponse
    systemDf.content == dataUsageResponse
  }

  def "ping"() {
    given:
    def systemApi = Mock(SystemApi)
    client.systemApi >> systemApi

    when:
    def ping = service.ping()

    then:
    1 * systemApi.systemPing() >> "OK"
    ping.content == "OK"
  }

  def "version"() {
    given:
    def systemApi = Mock(SystemApi)
    client.systemApi >> systemApi
    def systemVersion = Mock(SystemVersion)

    when:
    def version = service.version()

    then:
    1 * systemApi.systemVersion() >> systemVersion
    version.content == systemVersion
  }

  def "info"() {
    given:
    def systemApi = Mock(SystemApi)
    client.systemApi >> systemApi
    def systemInfo = Mock(SystemInfo)

    when:
    def info = service.info()

    then:
    1 * systemApi.systemInfo() >> systemInfo
    info.content == systemInfo
  }

  def "events"() {
    given:
    def systemApi = Mock(SystemApi)
    client.systemApi >> systemApi
    def systemEventsRequest = new SystemEventsRequest("since", "until", "filters")
    def callback = Mock(StreamCallback)
    def timeout = Duration.of(1, ChronoUnit.SECONDS)

    when:
    service.events(systemEventsRequest, callback, timeout)

    then:
    1 * systemApi.systemEvents("since", "until", "filters", callback, timeout.toMillis())
  }
}
