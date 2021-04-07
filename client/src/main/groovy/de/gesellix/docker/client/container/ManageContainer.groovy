package de.gesellix.docker.client.container

import de.gesellix.docker.client.DockerAsyncCallback
import de.gesellix.docker.engine.AttachConfig
import de.gesellix.docker.engine.EngineResponse
import okhttp3.WebSocket
import okhttp3.WebSocketListener

interface ManageContainer {

//    attach      Attach to a running container

  EngineResponse attach(container, query)

  EngineResponse attach(container, query, AttachConfig callback)

  EngineResponse resizeTTY(container, height, width)

  WebSocket attachWebsocket(container, query, WebSocketListener listener)

//    commit      Create a new image from a container's changes

  EngineResponse commit(container, query)

  EngineResponse commit(container, query, config)

//    cp          Copy files/folders between a container and the local filesystem

  def getArchiveStats(container, path)

  byte[] extractFile(String container, String filename)

  EngineResponse getArchive(String container, String path)

  EngineResponse putArchive(String container, String path, InputStream archive)

  EngineResponse putArchive(String container, String path, InputStream archive, Map<String, ?> query)

//    create      Create a new container

  EngineResponse createContainer(Map<String, ?> containerConfig)

  EngineResponse createContainer(Map<String, ?> containerConfig, Map<String, ?> query)

  EngineResponse createContainer(Map<String, ?> containerConfig, Map<String, ?> query, String authBase64Encoded)

//    diff        Inspect changes on a container's filesystem

  EngineResponse diff(container)

//    exec        Run a command in a running container

  EngineResponse createExec(container, Map execConfig)

  EngineResponse startExec(execId, Map execConfig)

  EngineResponse startExec(execId, Map execConfig, AttachConfig attachConfig)

  EngineResponse inspectExec(execId)

  EngineResponse exec(container, command)

  EngineResponse exec(container, command, Map execConfig)

  EngineResponse resizeExec(exec, height, width)

//    export      Export a container's filesystem as a tar archive

  EngineResponse export(container)

//    inspect     Display detailed information on one or more containers

  EngineResponse inspectContainer(container)

//    kill        Kill one or more running containers

  EngineResponse kill(container)

//    logs        Fetch the logs of a container

  EngineResponse logs(container)

  EngineResponse logs(container, DockerAsyncCallback callback)

  EngineResponse logs(container, query)

  EngineResponse logs(container, query, DockerAsyncCallback callback)

//    ls          List containers

  EngineResponse ps()

  EngineResponse ps(query)

//    pause       Pause all processes within one or more containers

  EngineResponse pause(container)

//    port        List port mappings or a specific mapping for the container

//    prune       Remove all stopped containers

  EngineResponse pruneContainers()

  EngineResponse pruneContainers(query)

//    rename      Rename a container

  EngineResponse rename(String container, String newName)

//    restart     Restart one or more containers

  EngineResponse restart(String containerIdOrName)

//    rm          Remove one or more containers

  EngineResponse rm(String containerIdOrName)

  EngineResponse rm(String containerIdOrName, query)

//    run         Run a command in a new container

  def run(String image, containerConfig)

  def run(String image, containerConfig, String tag)

  def run(String image, containerConfig, String tag, String name)

  def run(String image, containerConfig, String tag, String name, String authBase64Encoded)

//    start       Start one or more stopped containers

  EngineResponse startContainer(container)

//    stats       Display a live stream of container(s) resource usage statistics

  EngineResponse stats(container)

  EngineResponse stats(container, DockerAsyncCallback callback)

//    stop        Stop one or more running containers

  EngineResponse stop(String containerIdOrName)

  EngineResponse stop(String containerIdOrName, Integer timeout)

//    top         Display the running processes of a container

  EngineResponse top(String containerIdOrName)

  EngineResponse top(String containerIdOrName, ps_args)

//    unpause     Unpause all processes within one or more containers

  EngineResponse unpause(container)

//    update      Update configuration of one or more containers

  EngineResponse updateContainer(String container, containerConfig)

  Map<String, EngineResponse> updateContainers(List<String> containers, containerConfig)

//    wait        Block until one or more containers stop, then print their exit codes

  EngineResponse wait(String containerIdOrName)
}
