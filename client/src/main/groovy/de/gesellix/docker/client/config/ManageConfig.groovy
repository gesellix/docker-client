package de.gesellix.docker.client.config

import de.gesellix.docker.engine.EngineResponse

interface ManageConfig {

//    create      Create a config using bytes as content

    def createConfig(String name, byte[] configData)

    def createConfig(String name, byte[] configData, Map<String, String> labels)

//    inspect     Display detailed information on one or more configs

    def inspectConfig(String configId)

//    ls          List configs

    EngineResponse configs()

    EngineResponse configs(Map query)

//    rm          Remove one or more configs

    def rmConfig(String configId)

//    update      Update a Config

    def updateConfig(String configId, version, configSpec)
}
