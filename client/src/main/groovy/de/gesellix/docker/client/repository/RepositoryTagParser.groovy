package de.gesellix.docker.client.repository

import de.gesellix.docker.client.DockerClientException

class RepositoryTagParser {

    def parseRepositoryTag(name) {
        if (name.endsWith(':')) {
            throw new DockerClientException(new IllegalArgumentException("'$name' should not end with a ':'"))
        }

        // see https://github.com/docker/docker/blob/master/pkg/parsers/parsers.go#L72:
        // Get a repos name and returns the right reposName + tag
        // The tag can be confusing because of a port in a repository name.
        //     Ex: localhost.localdomain:5000/samalba/hipache:latest

        def lastColonIndex = name.lastIndexOf(':')
        if (lastColonIndex < 0) {
            return [
                    repo: name,
                    tag : ""
            ]
        }

        def tag = name.substring(lastColonIndex + 1)
        if (!tag.contains('/')) {
            return [
                    repo: name.substring(0, lastColonIndex),
                    tag : tag
            ]
        }

        return [
                repo: name,
                tag : ""
        ]
    }
}
