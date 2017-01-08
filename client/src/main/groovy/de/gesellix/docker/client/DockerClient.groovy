package de.gesellix.docker.client

interface DockerClient
        extends ManageCheckpoint,
                ManageContainer,
                ManageImage,
                ManageNetwork,
                ManageNode,
                ManagePlugin,
                ManageSecret,
                ManageService,
                ManageStack,
                ManageSwarm,
                ManageSystem,
                ManageVolume {

    def cleanupStorage(Closure shouldKeepContainer)

    def cleanupStorage(Closure shouldKeepContainer, Closure shouldKeepVolume)

    def cleanupImages()

    def cleanupContainers(Closure shouldKeepContainer)

    def cleanupVolumes(Closure shouldKeepVolume)

    def ping()

    def version()

    def readDefaultAuthConfig()

    def readAuthConfig(hostnameOrNull, File dockerCfgOrNull)

    def encodeAuthConfig(authConfig)

    def auth(authDetails)

    def parseRepositoryTag(name)

    def search(term)

    def inspectSwarm()

    def inspectSwarm(query)

    def getSwarmMangerAddress()

    def tasks()

    def tasks(query)

    def inspectTask(name)
}
