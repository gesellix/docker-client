package de.gesellix.docker.client.stack.types

import groovy.transform.ToString

@ToString
class StackService {

    String name
    def labels = [:]
    def endpointSpec = [:]
    def mode = [:]
    def taskTemplate = [:]
    def updateConfig = [:]
    def networks = [:]
}
