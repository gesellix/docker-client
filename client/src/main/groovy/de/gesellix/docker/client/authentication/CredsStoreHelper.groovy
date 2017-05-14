package de.gesellix.docker.client.authentication

import groovy.json.JsonException
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

import java.util.concurrent.TimeUnit

@Slf4j
class CredsStoreHelper {

    Map getAuthentication(String credsStore, String hostname = "https://index.docker.io/v1/") {

        def dockerCredentialResult = [:]

        def process
        try {
            process = new ProcessBuilder("docker-credential-${credsStore}", "get")
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .start()
        } catch (Exception exc) {
            log.error("error trying to execute docker-credential-${credsStore}", exc)
            dockerCredentialResult['auth'] = null
            dockerCredentialResult['error'] = exc.message
            return dockerCredentialResult
        }

        def buffer = new BufferedReader(new InputStreamReader(process.inputStream))

        process.outputStream.write(hostname?.bytes)
        process.outputStream.flush()
        process.outputStream.close()

        process.waitFor(10, TimeUnit.SECONDS)

        try {
            def auth = new JsonSlurper().parseText(buffer.readLines().join(""))
            dockerCredentialResult['auth'] = auth
            if (!dockerCredentialResult['auth']['ServerURL']) {
                dockerCredentialResult['auth']['ServerURL'] = hostname
            }
        } catch (JsonException exc) {
            log.error("cannot parse docker-credential-${credsStore} result for ${System.properties['user.name']}@${hostname}", exc)
            dockerCredentialResult['auth'] = null
            dockerCredentialResult['error'] = exc.message
        } catch (Exception exc) {
            log.error("error trying to get credentials from docker-credential-${credsStore}", exc)
            dockerCredentialResult['auth'] = null
            dockerCredentialResult['error'] = exc.message
        }

        return dockerCredentialResult
    }
}
