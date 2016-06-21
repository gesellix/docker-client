package de.gesellix.docker.client

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class DockerVersion {
    int major
    int minor
    int patch
    String meta

    @Override
    public String toString() {
        return "$major.$minor.$patch$meta"
    }
}
