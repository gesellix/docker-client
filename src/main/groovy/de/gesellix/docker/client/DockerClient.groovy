package de.gesellix.docker.client

interface DockerClient {

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

  def images()

  def createContainer(containerConfig)

  def createContainer(containerConfig, name)

  def startContainer(containerId)

  def run(containerConfig, fromImage)

  def run(containerConfig, fromImage, tag)

  def run(containerConfig, fromImage, tag, name)

  def stop(containerId)

  def rm(containerId)

  def rmi()
}
