package de.gesellix.docker.client.distribution

import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.client.HttpClient
import spock.lang.Specification

class ManageDistributionServiceTest extends Specification {

    HttpClient httpClient = Mock(HttpClient)
    ManageDistributionService service

    def setup() {
        service = new ManageDistributionService(httpClient, Mock(DockerResponseHandler))
    }

    def "distribution descriptor"() {
        when:
        service.descriptor("image-name")

        then:
        1 * httpClient.get([path: "/distribution/image-name/json"])
    }
}
