package de.gesellix.docker.client.stack

import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.client.HttpClient
import de.gesellix.docker.client.network.ManageNetwork
import de.gesellix.docker.client.node.ManageNode
import de.gesellix.docker.client.secret.ManageSecret
import de.gesellix.docker.client.service.ManageService
import de.gesellix.docker.client.system.ManageSystem
import de.gesellix.docker.client.tasks.ManageTask
import spock.lang.Specification

class ManageStackClientTest extends Specification {

    HttpClient httpClient = Mock(HttpClient)
    ManageStackClient service

    def setup() {
        service = new ManageStackClient(
                httpClient,
                Mock(DockerResponseHandler),
                Mock(ManageService),
                Mock(ManageTask),
                Mock(ManageNode),
                Mock(ManageNetwork),
                Mock(ManageSecret),
                Mock(ManageSystem))
    }
}
