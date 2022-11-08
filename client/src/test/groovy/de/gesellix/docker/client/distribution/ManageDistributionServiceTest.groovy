package de.gesellix.docker.client.distribution

import de.gesellix.docker.remote.api.DistributionInspect
import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.client.DistributionApi
import spock.lang.Specification

class ManageDistributionServiceTest extends Specification {

  EngineApiClient client = Mock(EngineApiClient)
  ManageDistributionService service

  def setup() {
    service = new ManageDistributionService(client)
  }

  def "distribution descriptor"() {
    given:
    def distributionApi = Mock(DistributionApi)
    client.distributionApi >> distributionApi
    def distributionInspect = Mock(DistributionInspect)

    when:
    def response = service.descriptor("image-name")

    then:
    1 * distributionApi.distributionInspect("image-name") >> distributionInspect
    response.content == distributionInspect
  }
}
