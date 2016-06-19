package de.gesellix.docker.client

import okhttp3.ws.WebSocketListener

interface DockerClient {

    def cleanupStorage(Closure shouldKeepContainer)

    def ping()

    def info()

    def version()

    def readDefaultAuthConfig()

    def readAuthConfig(hostnameOrNull, File dockerCfgOrNull)

    def encodeAuthConfig(authConfig)

    def auth(authDetails)

    def build(InputStream buildContext)

    def build(InputStream buildContext, query)

    def tag(image, repository)

    def parseRepositoryTag(name)

    def push(image)

    def push(image, authBase64Encoded)

    def push(image, authBase64Encoded, registry)

    def pull(image)

    def pull(image, tag)

    def pull(image, tag, authBase64Encoded)

    def pull(image, tag, authBase64Encoded, registry)

    def importUrl(url)

    def importUrl(url, repository)

    def importUrl(url, repository, tag)

    def importStream(stream)

    def importStream(stream, repository)

    def importStream(stream, repository, tag)

    def export(container)

    def save(... images)

    def load(stream)

    def ps()

    def ps(query)

    def inspectContainer(container)

    def diff(container)

    def inspectImage(image)

    def history(image)

    def images()

    def images(query)

    def createContainer(containerConfig)

    def createContainer(containerConfig, query)

    def startContainer(container)

    def updateContainer(container, containerConfig)

    def updateContainers(List containers, containerConfig)

    def run(image, containerConfig)

    def run(image, containerConfig, tag)

    def run(image, containerConfig, tag, name)

    def restart(container)

    def stop(container)

    def kill(container)

    def wait(container)

    def pause(container)

    def unpause(container)

    def rm(container)

    def rmi(image)

    def createExec(container, execConfig)

    def startExec(execId, execConfig)

    def inspectExec(execId)

    def exec(container, command)

    def exec(container, command, execConfig)

    def getArchiveStats(container, path)

    def extractFile(container, String filename)

    def getArchive(container, path)

    def putArchive(container, path, InputStream archive)

    def putArchive(container, path, InputStream archive, query)

    def rename(container, newName)

    def search(term)

    def attach(container, query)

    def attachWebsocket(container, query, WebSocketListener listener)

    def commit(container, query)

    def commit(container, query, config)

    def resizeTTY(container, height, width)

    def resizeExec(exec, height, width)

    def events(DockerAsyncCallback callback)

    def events(DockerAsyncCallback callback, query)

    def top(container)

    def top(container, ps_args)

    def stats(container)

    def stats(container, DockerAsyncCallback callback)

    def logs(container)

    def logs(container, DockerAsyncCallback callback)

    def logs(container, query)

    def logs(container, query, DockerAsyncCallback callback)

    def volumes()

    def volumes(query)

    def inspectVolume(name)

    def createVolume()

    def createVolume(config)

    def rmVolume(name)

    def networks()

    def networks(query)

    def inspectNetwork(name)

    def createNetwork(name)

    def createNetwork(name, config)

    def connectNetwork(network, container)

    def disconnectNetwork(network, container)

    def rmNetwork(name)

    def nodes()

    def nodes(query)

    def inspectNode(name)

    def updateNode(name, query, config)

    def rmNode(name)

    def inspectSwarm()

    def inspectSwarm(query)

    def initSwarm(config)

    def joinSwarm(config)

    def leaveSwarm()

    def leaveSwarm(query)

    def updateSwarm(query, config)

    def services()

    def services(query)

    def createService(config)

    def rmService(name)

    def inspectService(name)

    def updateService(name, query, config)

    def tasks()

    def tasks(query)

    def inspectTask(name)
}
