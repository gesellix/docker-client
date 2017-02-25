package de.gesellix.docker.client.stack

import de.gesellix.docker.client.stack.types.StackNetwork
import de.gesellix.docker.client.stack.types.StackSecret
import de.gesellix.docker.client.stack.types.StackService
import de.gesellix.docker.client.stack.types.StackVolume

class DeployStackConfig {

    Map<String, StackService> services = [:]
    Map<String, StackNetwork> networks = [:]
    Map<String, StackVolume> volumes = [:]
    Map<String, StackSecret> secrets = [:]
}
