package de.gesellix.docker.client.distribution;

import de.gesellix.docker.engine.EngineResponse;

public interface ManageDistribution {

  EngineResponse descriptor(String image);
}
