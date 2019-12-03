package de.gesellix.docker.testutil

import de.gesellix.docker.client.DockerClientImpl
import de.gesellix.docker.client.LocalDocker
import de.gesellix.docker.client.container.ArchiveUtil

class NpipeExecutable {

    File createNpipeExe(DockerClientImpl docker) {
        def repository = LocalDocker.isNativeWindows() ? "gesellix/npipe:windows" : "gesellix/npipe"
        docker.createContainer([Image: repository], [name: "npipe"])
        def archive = docker.getArchive("npipe", "/npipe.exe").stream as InputStream

        def npipeExe = new File("npipe.exe")
        new ArchiveUtil().copySingleTarEntry(archive, "/npipe.exe", new FileOutputStream(npipeExe))
        return npipeExe
    }
}
