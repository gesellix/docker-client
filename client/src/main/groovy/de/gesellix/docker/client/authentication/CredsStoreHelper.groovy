package de.gesellix.docker.client.authentication

import groovy.json.JsonException
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

import java.util.concurrent.TimeUnit

@Slf4j
class CredsStoreHelper {

    Map getAuthentication(String credsStore, String hostname = "https://index.docker.io/v1/") {

        def dockerCredentialResult = [:]

        def result = readFromCredsStore(credsStore, hostname)

        if (!result.success) {
            dockerCredentialResult['auth'] = null
            dockerCredentialResult['error'] = result.message
            return dockerCredentialResult
        }

        try {
            def auth = new JsonSlurper().parseText(result.message)
            dockerCredentialResult['auth'] = auth
            if (!dockerCredentialResult['auth']['ServerURL']) {
                dockerCredentialResult['auth']['ServerURL'] = hostname
            }
        }
        catch (JsonException exc) {
            log.error("cannot parse docker-credential-${credsStore} result for ${System.properties['user.name']}@${hostname}", exc)
            dockerCredentialResult['auth'] = null
            dockerCredentialResult['error'] = exc.message
        }
        catch (Exception exc) {
            log.error("error trying to get credentials from docker-credential-${credsStore}", exc)
            dockerCredentialResult['auth'] = null
            dockerCredentialResult['error'] = exc.message
        }

        return dockerCredentialResult
    }

    CredsStoreResult readFromCredsStore(String credsStore, String hostname) {
        def process
        try {
            process = new ProcessBuilder("docker-credential-${credsStore}", "get")
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .start()
        }
        catch (Exception exc) {
            log.error("error trying to execute docker-credential-${credsStore}", exc)
            return new CredsStoreResult(
                    success: false,
                    message: exc.message
            )
        }

        def buffer = new BufferedReader(new InputStreamReader(process.inputStream))

        process.outputStream.write(hostname?.bytes)
        process.outputStream.flush()
        process.outputStream.close()

        process.waitFor(10, TimeUnit.SECONDS)

        if (process.exitValue() != 0) {
            log.error("docker-credential-${credsStore} failed")
        }
        return new CredsStoreResult(
                success: process.exitValue() == 0,
                message: buffer.readLines().join('')
        )
    }

    static class CredsStoreResult {

        boolean success
        String message
    }
}
