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

  def tag(imageId, repository)

  def tag(imageId, repository, force)

  def parseRepositoryTag(name)

  def push(imageName)

  def push(imageName, authBase64Encoded)

  def push(imageName, authBase64Encoded, registry)

  def pull(imageName)

  def pull(imageName, tag)

  def pull(imageName, tag, authBase64Encoded)

  def pull(imageName, tag, authBase64Encoded, registry)

  def ps()

  def ps(query)

  def inspectContainer(containerId)

  def diff(containerId)

  def inspectImage(imageId)

  def history(imageId)

  def images()

  def images(query)

  def createContainer(containerConfig)

  def createContainer(containerConfig, query)

  def startContainer(containerId)

  def run(fromImage, containerConfig)

  def run(fromImage, containerConfig, tag)

  def run(fromImage, containerConfig, tag, name)

  def restart(containerId)

  def stop(containerId)

  def kill(containerId)

  def wait(containerId)

  def pause(containerId)

  def unpause(containerId)

  def rm(containerId)

  def rmi(imageId)

  def createExec(containerId, execConfig)

  def startExec(execId, execConfig)

  def exec(containerId, command)

  def exec(containerId, command, execConfig)

  def copy(containerId, resourceBody)

  def copyFile(containerId, String filename)

  def rename(containerId, newName)

  def search(term)

  def attach(containerId, query)

  def attachWebsocket(containerId, query, handler)
}
