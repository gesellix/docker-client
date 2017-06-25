package de.gesellix.docker.engine

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class DockerVersion implements Comparable<DockerVersion> {

    int major
    int minor
    int patch
    String meta

    static DockerVersion parseDockerVersion(String version) {
        final versionPattern = /(\d+)\.(\d+)(?:\.(\d+)(.*))?/

        def parsedVersion = new DockerVersion()
        version.eachMatch(versionPattern) { List<String> groups ->
            parsedVersion.major = Integer.parseInt(groups[1])
            parsedVersion.minor = Integer.parseInt(groups[2])
            parsedVersion.patch = Integer.parseInt(groups[3] ?: "0")
            parsedVersion.meta = groups[4] ?: ""
        }

        return parsedVersion
    }

    @Override
    String toString() {
        return "$major.$minor.$patch$meta"
    }

    @Override
    int compareTo(DockerVersion other) {
        def self = [this.major, this.minor, this.patch]
        def that = [other.major, other.minor, other.patch]

        def result = 0
        (0..2).each { index ->
            def compared = self[index] <=> that[index]
            if (compared != 0 && result == 0) {
                result = compared
            }
        }
        return result
    }
}
