package de.gesellix.docker.client.config

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class DockerVersion {

    int major
    int minor
    int patch
    String meta

    static DockerVersion parseDockerVersion(String version) {
        final versionPattern = /(\d+)\.(\d+)\.(\d+)(.*)/

        def parsedVersion = new DockerVersion()
        version.eachMatch(versionPattern) { List<String> groups ->
            parsedVersion.major = Integer.parseInt(groups[1])
            parsedVersion.minor = Integer.parseInt(groups[2])
            parsedVersion.patch = Integer.parseInt(groups[3])
            parsedVersion.meta = groups[4]
        }

        return parsedVersion
    }

    @Override
    String toString() {
        return "$major.$minor.$patch$meta"
    }
}
