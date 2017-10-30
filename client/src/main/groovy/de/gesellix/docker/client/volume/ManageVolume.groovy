package de.gesellix.docker.client.volume

import de.gesellix.docker.engine.EngineResponse

interface ManageVolume {

//    create      Create a volume

    EngineResponse createVolume()

    EngineResponse createVolume(config)

//    inspect     Display detailed information on one or more volumes

    EngineResponse inspectVolume(name)

//    ls          List volumes

    EngineResponse volumes()

    EngineResponse volumes(query)

//    prune       Remove all unused volumes

    EngineResponse pruneVolumes()

    EngineResponse pruneVolumes(query)

//    rm          Remove one or more volumes

    EngineResponse rmVolume(name)
}
