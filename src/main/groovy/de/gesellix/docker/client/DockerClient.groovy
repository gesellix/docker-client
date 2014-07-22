package de.gesellix.docker.client

interface DockerClient {

  def info()

  def version()

  def auth(authDetails)

  def encodeAuthConfig(authConfig)

  def build(InputStream buildContext)

  def build(InputStream buildContext, removeIntermediateContainers)

  def tag(imageId, name)

  def push(imageName, authBase64Encoded)

  def push(imageName, authBase64Encoded, registry)

  def pull(imageName)

  def pull(imageName, tag)

  def pull(imageName, tag, registry)

  def ps()

  def inspectContainer(containerId)

  def images()

  def createContainer(containerConfig)

  def createContainer(containerConfig, name)

  def startContainer(containerId)

  def startContainer(containerId, hostConfig)

  def run(fromImage, containerConfig, hostConfig)

  def run(fromImage, containerConfig, hostConfig, tag)

  def run(fromImage, containerConfig, hostConfig, tag, name)

  def stop(containerId)

  def wait(containerId)

  def rm(containerId)

  def rmi()
}
