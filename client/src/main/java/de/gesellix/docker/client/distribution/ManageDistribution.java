package de.gesellix.docker.client.distribution;

import de.gesellix.docker.engine.EngineResponse;
import de.gesellix.docker.remote.api.DistributionInspect;

public interface ManageDistribution {

  EngineResponse<DistributionInspect> descriptor(String image);
}
