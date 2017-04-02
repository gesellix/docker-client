package de.gesellix.docker.client.secret

import de.gesellix.docker.client.DockerResponse

interface ManageSecret {

//    create      Create a secret using bytes as content

    def createSecret(String name, byte[] secretData)

    def createSecret(String name, byte[] secretData, Map<String, String> labels)

//    inspect     Display detailed information on one or more secrets

    def inspectSecret(String secretId)

//    ls          List secrets

    DockerResponse secrets()

    DockerResponse secrets(Map query)

//    rm          Remove one or more secrets

    def rmSecret(String secretId)

//    update      Update a Secret

    def updateSecret(String secretId, version, secretSpec)
}
