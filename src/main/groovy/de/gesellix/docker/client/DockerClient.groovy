package de.gesellix.docker.client

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

    def tag(image, repository, force)

    def parseRepositoryTag(name)

    def push(image)

    def push(image, authBase64Encoded)

    def push(image, authBase64Encoded, registry)

    def pull(image)

    def pull(image, tag)

    def pull(image, tag, authBase64Encoded)

    def pull(image, tag, authBase64Encoded, registry)

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

    def exec(container, command)

    def exec(container, command, execConfig)

    def copy(container, resourceBody)

    def copyFile(container, String filename)

    def getArchiveInfo(container, path)

    def downloadArchive(container, path)

    def uploadArchive(container, path, file)

    def rename(container, newName)

    def search(term)

    def attach(container, query)

    def attachWebsocket(container, query, handler)
}
