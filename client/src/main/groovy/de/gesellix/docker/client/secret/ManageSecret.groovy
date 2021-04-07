package de.gesellix.docker.client.secret

import de.gesellix.docker.engine.EngineResponse

interface ManageSecret {

//    create      Create a secret using bytes as content

  EngineResponse createSecret(String name, byte[] secretData)

  EngineResponse createSecret(String name, byte[] secretData, Map<String, String> labels)

//    inspect     Display detailed information on one or more secrets

  EngineResponse inspectSecret(String secretId)

//    ls          List secrets

  EngineResponse secrets()

  EngineResponse secrets(Map query)

//    rm          Remove one or more secrets

  EngineResponse rmSecret(String secretId)

//    update      Update a Secret

  EngineResponse updateSecret(String secretId, version, secretSpec)
}
