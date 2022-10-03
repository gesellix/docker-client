package de.gesellix.docker.client.repository;

import de.gesellix.docker.client.DockerClientException;

public class RepositoryTagParser {

  public RepositoryAndTag parseRepositoryTag(String name) {
    if (name.endsWith(":")) {
      throw new DockerClientException(new IllegalArgumentException(String.format("'%s' should not end with a ':'", name)));
    }

    // see https://github.com/docker/docker/blob/master/pkg/parsers/parsers.go#L72:
    // Get a repos name and returns the right reposName + tag
    // The tag can be confusing because of a port in a repository name.
    //     Ex: localhost.localdomain:5000/samalba/hipache:latest

    int lastColonIndex = name.lastIndexOf(":");
    if (lastColonIndex < 0) {
      return new RepositoryAndTag(name, "");
    }

    String tag = name.substring(lastColonIndex + 1);
    if (!tag.contains("/")) {
      return new RepositoryAndTag(name.substring(0, lastColonIndex), tag);
    }

    return new RepositoryAndTag(name, "");
  }
}
