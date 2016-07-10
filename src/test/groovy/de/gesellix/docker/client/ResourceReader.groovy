package de.gesellix.docker.client

class ResourceReader {

    def getClasspathResourceAsFile(String classpathResource) {
        def resource = getClass().getResource(classpathResource)
        return new File(resource.toURI())
    }
}
