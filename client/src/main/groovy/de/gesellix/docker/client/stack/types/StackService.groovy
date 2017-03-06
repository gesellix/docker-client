package de.gesellix.docker.client.stack.types

import groovy.transform.ToString

@ToString
class StackService {

    def endpointSpec = [:]
    def mode = [:]
    def taskTemplate = [:]
    def networks = [:]
}
