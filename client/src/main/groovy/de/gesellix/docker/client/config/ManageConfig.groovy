package de.gesellix.docker.client.config

import de.gesellix.docker.engine.EngineResponse

interface ManageConfig {

//    create      Create a config using bytes as content

  EngineResponse createConfig(String name, byte[] configData)

  EngineResponse createConfig(String name, byte[] configData, Map<String, String> labels)

//    inspect     Display detailed information on one or more configs

  EngineResponse inspectConfig(String configId)

//    ls          List configs

  EngineResponse configs()

  EngineResponse configs(Map query)

//    rm          Remove one or more configs

  EngineResponse rmConfig(String configId)

//    update      Update a Config

  EngineResponse updateConfig(String configId, version, configSpec)
}
