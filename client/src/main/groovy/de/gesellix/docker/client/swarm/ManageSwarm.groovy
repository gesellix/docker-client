package de.gesellix.docker.client.swarm

interface ManageSwarm {

    Map newSwarmConfig()

//    init        Initialize a swarm

    def initSwarm()

    def initSwarm(Map config)

//    join        Join a swarm as a node and/or manager

    def joinSwarm(Map config)

//    join-token  Manage join tokens

    def inspectSwarm()

    def inspectSwarm(Map query)

    def getSwarmWorkerToken()

    def rotateSwarmWorkerToken()

    def getSwarmManagerToken()

    def rotateSwarmManagerToken()

//    leave       Leave the swarm

    def leaveSwarm()

    def leaveSwarm(Map query)

//    unlock      Unlock swarm

    def unlockSwarm(String unlockKey)

//    unlock-key  Manage the unlock key

    def getSwarmManagerUnlockKey()

    def rotateSwarmManagerUnlockKey()

//    update      Update the swarm

    def updateSwarm(Map query, Map config)
}
