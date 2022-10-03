package de.gesellix.docker.testutil

import de.gesellix.docker.client.DockerClientImpl
import de.gesellix.docker.client.container.ArchiveUtil
import de.gesellix.docker.remote.api.ContainerCreateRequest

class NpipeExecutable {

  File createNpipeExe(DockerClientImpl docker) {
    String repository = "gesellix/npipe:2022-07-31T14-30-00"
    docker.createContainer(new ContainerCreateRequest().tap { image = repository }, "npipe")
    InputStream archive = docker.getArchive("npipe", "/npipe.exe").content
    def npipeExe = new File("npipe.exe")
    new ArchiveUtil().copySingleTarEntry(archive, "/npipe.exe", new FileOutputStream(npipeExe))
    return npipeExe
  }
}
