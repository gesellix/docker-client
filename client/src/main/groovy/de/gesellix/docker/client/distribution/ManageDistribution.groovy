package de.gesellix.docker.client.distribution

import de.gesellix.docker.engine.EngineResponse

interface ManageDistribution {

//    * `GET /distribution/(name)/json` is a new endpoint that returns a JSON output stream with payload `types.DistributionInspect` for an image name. It includes a descriptor with the digest, and supported platforms retrieved from directly contacting the registry.

  EngineResponse descriptor(String image)
}
