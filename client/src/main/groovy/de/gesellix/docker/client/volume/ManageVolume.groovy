package de.gesellix.docker.client.volume

interface ManageVolume {

//    create      Create a volume

    def createVolume()

    def createVolume(config)

//    inspect     Display detailed information on one or more volumes

    def inspectVolume(name)

//    ls          List volumes

    def volumes()

    def volumes(query)

//    prune       Remove all unused volumes

    def pruneVolumes()

    def pruneVolumes(query)

//    rm          Remove one or more volumes

    def rmVolume(name)

}
