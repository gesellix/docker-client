package de.gesellix.docker.client

interface DockerClient {

  def auth(authDetails)

  def build(InputStream buildContext)

  def tag(imageId, repositoryName)

  def push(repositoryName, auth)

  def pull(imageName)

  def stop()

  def rm()

  def rmi()

  def run()

  def ps()

  def images()

  def createContainer(fromImage, cmd)

  def startContainer(containerId)
}
