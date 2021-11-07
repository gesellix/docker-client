package de.gesellix.docker.client.swarm

import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.JoinTokens
import de.gesellix.docker.remote.api.ObjectVersion
import de.gesellix.docker.remote.api.Swarm
import de.gesellix.docker.remote.api.SwarmInitRequest
import de.gesellix.docker.remote.api.SwarmJoinRequest
import de.gesellix.docker.remote.api.SwarmSpec
import de.gesellix.docker.remote.api.SwarmUnlockRequest
import de.gesellix.docker.remote.api.UnlockKeyResponse
import de.gesellix.docker.remote.api.client.SwarmApi
import io.github.joke.spockmockable.Mockable
import spock.lang.Specification

@Mockable([SwarmApi, Swarm, SwarmInitRequest, SwarmJoinRequest, SwarmSpec, JoinTokens, ObjectVersion, UnlockKeyResponse])
class ManageSwarmClientTest extends Specification {

  EngineApiClient client = Mock(EngineApiClient)
  ManageSwarmClient service

  def setup() {
    service = new ManageSwarmClient(client)
  }

  def "inspect swarm"() {
    given:
    def swarmApi = Mock(SwarmApi)
    client.swarmApi >> swarmApi
    def swarmInfo = Mock(Swarm)

    when:
    def responseContent = service.inspectSwarm()

    then:
    1 * swarmApi.swarmInspect() >> swarmInfo
    responseContent.content == swarmInfo
  }

  def "initialize a swarm"() {
    given:
    def swarmApi = Mock(SwarmApi)
    client.swarmApi >> swarmApi
    def initRequest = Mock(SwarmInitRequest)

    when:
    def responseContent = service.initSwarm(initRequest)

    then:
    1 * swarmApi.swarmInit(initRequest) >> "node-id"
    responseContent.content == "node-id"
  }

  def "join a swarm"() {
    given:
    def swarmApi = Mock(SwarmApi)
    client.swarmApi >> swarmApi
    def joinRequest = Mock(SwarmJoinRequest)

    when:
    service.joinSwarm(joinRequest)

    then:
    1 * swarmApi.swarmJoin(joinRequest)
  }

  def "leave a swarm"() {
    given:
    def swarmApi = Mock(SwarmApi)
    client.swarmApi >> swarmApi

    when:
    service.leaveSwarm(true)

    then:
    1 * swarmApi.swarmLeave(true)
  }

  def "update a swarm"() {
    given:
    def swarmApi = Mock(SwarmApi)
    client.swarmApi >> swarmApi
    def version = 42
    def swarmSpec = Mock(SwarmSpec)

    when:
    service.updateSwarm(version, swarmSpec, null, true, false)

    then:
    1 * swarmApi.swarmUpdate(version, swarmSpec, null, true, false)
  }

  def "get the swarm worker token"() {
    given:
    def swarmApi = Mock(SwarmApi)
    client.swarmApi >> swarmApi
    def swarm = Mock(Swarm, { joinTokens >> Mock(JoinTokens, { worker >> "worker-token" }) })

    when:
    def token = service.getSwarmWorkerToken()

    then:
    1 * swarmApi.swarmInspect() >> swarm
    token == "worker-token"
  }

  def "rotate the swarm worker token"() {
    given:
    def swarmApi = Mock(SwarmApi)
    client.swarmApi >> swarmApi
    def swarmSpec = Mock(SwarmSpec)

    when:
    def token = service.rotateSwarmWorkerToken()

    then:
    1 * swarmApi.swarmInspect() >> Mock(Swarm, {
      version >> Mock(ObjectVersion, { index >> 21 })
      spec >> swarmSpec
      joinTokens >> Mock(JoinTokens, { worker >> "worker-token-1" })
    })
    then:
    1 * swarmApi.swarmUpdate(21, swarmSpec, true, false, false)
    then:
    1 * swarmApi.swarmInspect() >> Mock(Swarm, {
      version >> Mock(ObjectVersion, { index >> 23 })
      spec >> swarmSpec
      joinTokens >> Mock(JoinTokens, { worker >> "worker-token-2" })
    })
    and:
    token == "worker-token-2"
  }

  def "get the swarm manager token"() {
    given:
    def swarmApi = Mock(SwarmApi)
    client.swarmApi >> swarmApi
    def swarm = Mock(Swarm, { joinTokens >> Mock(JoinTokens, { manager >> "manager-token" }) })

    when:
    def token = service.getSwarmManagerToken()

    then:
    1 * swarmApi.swarmInspect() >> swarm
    token == "manager-token"
  }

  def "rotate the swarm manager token"() {
    given:
    def swarmApi = Mock(SwarmApi)
    client.swarmApi >> swarmApi
    def swarmSpec = Mock(SwarmSpec)

    when:
    def token = service.rotateSwarmManagerToken()

    then:
    1 * swarmApi.swarmInspect() >> Mock(Swarm, {
      version >> Mock(ObjectVersion, { index >> 78 })
      spec >> swarmSpec
      joinTokens >> Mock(JoinTokens, { manager >> "manager-token-1" })
    })
    then:
    1 * swarmApi.swarmUpdate(78, swarmSpec, false, true, false)
    then:
    1 * swarmApi.swarmInspect() >> Mock(Swarm, {
      version >> Mock(ObjectVersion, { index >> 79 })
      spec >> swarmSpec
      joinTokens >> Mock(JoinTokens, { manager >> "manager-token-2" })
    })
    and:
    token == "manager-token-2"
  }

  def "get the swarm manager unlock key"() {
    given:
    def swarmApi = Mock(SwarmApi)
    client.swarmApi >> swarmApi
    def unlockKeyResponse = Mock(UnlockKeyResponse, { unlockKey >> "manager-unlock-key" })

    when:
    def unlockKey = service.getSwarmManagerUnlockKey()

    then:
    1 * swarmApi.swarmUnlockkey() >> unlockKeyResponse
    and:
    unlockKey == "manager-unlock-key"
  }

  def "rotate the swarm manager unlock key"() {
    given:
    def swarmApi = Mock(SwarmApi)
    client.swarmApi >> swarmApi
    def swarmSpec = Mock(SwarmSpec)
    def unlockKeyResponse = Mock(UnlockKeyResponse, { unlockKey >> "manager-unlock-key" })

    when:
    def unlockKey = service.rotateSwarmManagerUnlockKey()

    then:
    1 * swarmApi.swarmInspect() >> Mock(Swarm, {
      version >> Mock(ObjectVersion, { index >> 11 })
      spec >> swarmSpec
    })
    then:
    1 * swarmApi.swarmUpdate(11, swarmSpec, false, false, true)
    then:
    1 * swarmApi.swarmUnlockkey() >> unlockKeyResponse
    and:
    unlockKey == "manager-unlock-key"
  }

  def "unlock swarm"() {
    given:
    def swarmApi = Mock(SwarmApi)
    client.swarmApi >> swarmApi
    def unlockKey = "SWMKEY-1-4711"

    when:
    service.unlockSwarm(unlockKey)

    then:
    1 * swarmApi.swarmUnlock(new SwarmUnlockRequest(unlockKey))
  }
}
