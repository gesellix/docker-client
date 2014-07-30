package de.gesellix.docker.client

class ResourceReader {

  def getClasspathResourceAsFile(classpathResource) {
    def resource = getClass().getResource(classpathResource)
    return new File(resource.toURI())
  }
}
