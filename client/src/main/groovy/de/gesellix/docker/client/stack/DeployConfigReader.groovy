package de.gesellix.docker.client.stack

import de.gesellix.docker.compose.ComposeFileReader
import de.gesellix.docker.compose.types.ComposeConfig
import groovy.util.logging.Slf4j

@Slf4j
class DeployConfigReader {

    ComposeFileReader composeFileReader = new ComposeFileReader()

    def loadCompose(InputStream composeFile) {
        ComposeConfig composeConfig = composeFileReader.load(composeFile)
        log.info("composeContent: $composeConfig}")

        def cfg = new DeployStackConfig()
        return cfg
    }

}
