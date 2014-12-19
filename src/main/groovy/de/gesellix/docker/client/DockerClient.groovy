package de.gesellix.docker.client

interface DockerClient {

  def info()

  def version()

  def auth(authDetails)

  def encodeAuthConfig(authConfig)

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

  def pull(imageName, tag, registry)

  def ps()

  def inspectContainer(containerId)

  def images()

  def images(query)

  def createContainer(containerConfig)

  def createContainer(containerConfig, query)

  def startContainer(containerId)

  def startContainer(containerId, hostConfig)

  def run(fromImage, containerConfig, hostConfig)

  def run(fromImage, containerConfig, hostConfig, tag)

  def run(fromImage, containerConfig, hostConfig, tag, name)

  def stop(containerId)

  def wait(containerId)

  def rm(containerId)

  def rmi(imageId)
}
