package de.gesellix.docker.client.stack

import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.stack.types.StackSecret
import de.gesellix.docker.compose.types.External
import de.gesellix.docker.compose.types.Secret
import spock.lang.Specification

class DeployConfigReaderTest extends Specification {

    DeployConfigReader reader

    def setup() {
        reader = new DeployConfigReader(Mock(DockerClient))
    }

    def "converts secrets"() {
        given:
        def secret1 = new Secret()
        def secret1File = getClass().getResource('/secrets/secret1.txt').file
        secret1.file = secret1File

        when:
        def result = reader.secrets("name-space", [
                'secret-1'  : secret1,
                'ext-secret': new Secret(external: new External(external: true))])

        then:
        result == [
                'secret-1': new StackSecret(
                        data: new FileInputStream(secret1File).bytes,
                        name: 'name-space_secret-1',
                        labels: ['com.docker.stack.namespace': 'name-space'])
        ]
    }

}
