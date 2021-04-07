package de.gesellix.docker.client.image

import de.gesellix.docker.engine.EngineResponse

class BuildResult {

  String imageId
  List<Object> log

  EngineResponse response
}
