package de.gesellix.docker.client.container

import de.gesellix.docker.client.AttachConfig
import de.gesellix.docker.client.DockerAsyncCallback
import de.gesellix.docker.client.DockerResponse
import okhttp3.WebSocketListener

interface ManageContainer {

//    attach      Attach to a running container

    def attach(container, query)

    def attach(container, query, AttachConfig callback)

    def resizeTTY(container, height, width)

    def attachWebsocket(container, query, WebSocketListener listener)

//    commit      Create a new image from a container's changes

    def commit(container, query)

    def commit(container, query, config)

//    cp          Copy files/folders between a container and the local filesystem

    def getArchiveStats(container, path)

    byte[] extractFile(container, String filename)

    def getArchive(container, path)

    def putArchive(container, path, InputStream archive)

    def putArchive(container, path, InputStream archive, query)

//    create      Create a new container

    DockerResponse createContainer(containerConfig)

    DockerResponse createContainer(containerConfig, query)

//    diff        Inspect changes on a container's filesystem

    def diff(container)

//    exec        Run a command in a running container

    def createExec(container, Map execConfig)

    def startExec(execId, Map execConfig)

    def startExec(execId, Map execConfig, AttachConfig attachConfig)

    def inspectExec(execId)

    def exec(container, command)

    def exec(container, command, Map execConfig)

    def resizeExec(exec, height, width)

//    export      Export a container's filesystem as a tar archive

    def export(container)

//    inspect     Display detailed information on one or more containers

    def inspectContainer(container)

//    kill        Kill one or more running containers

    def kill(container)

//    logs        Fetch the logs of a container

    def logs(container)

    def logs(container, DockerAsyncCallback callback)

    def logs(container, query)

    def logs(container, query, DockerAsyncCallback callback)

//    ls          List containers

    def ps()

    def ps(query)

//    pause       Pause all processes within one or more containers

    def pause(container)

//    port        List port mappings or a specific mapping for the container

//    prune       Remove all stopped containers

    def pruneContainers()

    def pruneContainers(query)

//    rename      Rename a container

    def rename(container, newName)

//    restart     Restart one or more containers

    def restart(container)

//    rm          Remove one or more containers

    def rm(container)

    def rm(container, query)

//    run         Run a command in a new container

    def run(image, containerConfig)

    def run(image, containerConfig, tag)

    def run(image, containerConfig, tag, name)

//    start       Start one or more stopped containers

    def startContainer(container)

//    stats       Display a live stream of container(s) resource usage statistics

    def stats(container)

    def stats(container, DockerAsyncCallback callback)

//    stop        Stop one or more running containers

    def stop(container)

//    top         Display the running processes of a container

    def top(container)

    def top(container, ps_args)

//    unpause     Unpause all processes within one or more containers

    def unpause(container)

//    update      Update configuration of one or more containers

    def updateContainer(container, containerConfig)

    def updateContainers(List containers, containerConfig)

//    wait        Block until one or more containers stop, then print their exit codes

    def wait(container)

}
