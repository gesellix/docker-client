package de.gesellix.docker.client

interface ManageSecret {

//    create      Create a secret using bytes as content

    def createSecret(String name, byte[] secretData)

    def createSecret(String name, byte[] secretData, Map<String, String> labels)

//    inspect     Display detailed information on one or more secrets

    def inspectSecret(String secretId)

//    ls          List secrets

    def secrets()

//    TODO rm          Remove one or more secrets

}
