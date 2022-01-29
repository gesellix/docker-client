package de.gesellix.docker.client.distribution;

import de.gesellix.docker.client.EngineResponseContent;
import de.gesellix.docker.remote.api.DistributionInspect;
import de.gesellix.docker.remote.api.EngineApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManageDistributionService implements ManageDistribution {

  private static final Logger log = LoggerFactory.getLogger(ManageDistributionService.class);

  private final EngineApiClient client;

  public ManageDistributionService(EngineApiClient client) {
    this.client = client;
  }

  @Override
  public EngineResponseContent<DistributionInspect> descriptor(String image) {
    log.info("docker distribution descriptor");
    DistributionInspect distributionInspect = client.getDistributionApi().distributionInspect(image);
    return new EngineResponseContent<>(distributionInspect);
  }
}
