package de.gesellix.docker.client.distribution

import de.gesellix.docker.client.EngineResponseContent
import de.gesellix.docker.remote.api.DistributionInspect
import de.gesellix.docker.remote.api.EngineApiClient
import groovy.util.logging.Slf4j

@Slf4j
class ManageDistributionService implements ManageDistribution {

  private EngineApiClient client

  ManageDistributionService(EngineApiClient client) {
    this.client = client
  }

  @Override
  EngineResponseContent<DistributionInspect> descriptor(String image) {
    log.info("docker distribution descriptor")
    def distributionInspect = client.getDistributionApi().distributionInspect(image)
    return new EngineResponseContent(distributionInspect)
  }
}
