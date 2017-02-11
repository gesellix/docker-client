package de.gesellix.docker.testutil

class ResourceReader {

    def getClasspathResourceAsFile(String classpathResource, Class baseClass) {
        def resource = baseClass.getResource(classpathResource)
        return new File(resource.toURI())
    }
}
