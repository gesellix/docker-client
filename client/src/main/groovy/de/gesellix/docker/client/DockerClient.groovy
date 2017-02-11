package de.gesellix.docker.client

import de.gesellix.docker.client.checkpoint.ManageCheckpoint
import de.gesellix.docker.client.container.ManageContainer
import de.gesellix.docker.client.image.ManageImage
import de.gesellix.docker.client.network.ManageNetwork
import de.gesellix.docker.client.node.ManageNode
import de.gesellix.docker.client.plugin.ManagePlugin
import de.gesellix.docker.client.secret.ManageSecret
import de.gesellix.docker.client.service.ManageService
import de.gesellix.docker.client.stack.ManageStack
import de.gesellix.docker.client.swarm.ManageSwarm
import de.gesellix.docker.client.system.ManageSystem
import de.gesellix.docker.client.volume.ManageVolume

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
