package de.gesellix.docker.client.swarm

interface ManageSwarm {

    def newSwarmConfig()

//    init        Initialize a swarm

    def initSwarm()

    def initSwarm(config)

//    join        Join a swarm as a node and/or manager

    def joinSwarm(config)

//    join-token  Manage join tokens

    def inspectSwarm()

    def inspectSwarm(query)

    def getSwarmWorkerToken()

    def rotateSwarmWorkerToken()

    def getSwarmManagerToken()

    def rotateSwarmManagerToken()

//    leave       Leave the swarm

    def leaveSwarm()

    def leaveSwarm(query)

//    unlock      Unlock swarm

    def unlockSwarm(unlockKey)

//    unlock-key  Manage the unlock key

    def getSwarmManagerUnlockKey()

    def rotateSwarmManagerUnlockKey()

//    update      Update the swarm

    def updateSwarm(query, config)

}
