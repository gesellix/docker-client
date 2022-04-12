package de.gesellix.docker.client.distribution;

import de.gesellix.docker.client.EngineResponseContent;
import de.gesellix.docker.remote.api.DistributionInspect;

public interface ManageDistribution {

  EngineResponseContent<DistributionInspect> descriptor(String image);
}
