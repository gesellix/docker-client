package de.gesellix.docker.client.stack.types

enum MountPropagation {

    PropagationRPrivate("rprivate"),
    PropagationPrivate("private"),
    PropagationRShared("rshared"),
    PropagationShared("shared"),
    PropagationRSlave("rslave"),
    PropagationSlave("slave")

    final String value

    MountPropagation(String value) {
        this.value = value
    }
}
