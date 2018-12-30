import java.io.ByteArrayOutputStream

evaluationDependsOn(":client")

gradle.projectsEvaluated({

    tasks.register<JavaExec>("checkDockerClient") {
        main = "de.gesellix.docker.client.LocalDocker"
        val clientSourceSets = project(":client").extensions.getByType(SourceSetContainer::class)
        classpath = clientSourceSets["main"].runtimeClasspath + clientSourceSets["test"].runtimeClasspath

        standardOutput = ByteArrayOutputStream()

        val environmentVariables = listOf(
                "DOCKER_HOST",
                "DOCKER_TLS_VERIFY"
        )
        val systemProperties = listOf(
                "java.version",
                "java.vendor",
                "os.name",
                "os.arch",
                "os.version"
        )

        fun summary(): String {
            var result = listOf("\nenvironment:\n")
            result += environmentVariables.map {
                "- $it: ${environment[it]}"
            }
            result += systemProperties.map {
                "- $it: ${System.getProperty(it)}"
            }
            return result.joinToString("\n")
        }

        doFirst {
            logger.lifecycle("running availability check...")
        }
        doLast {
            val log = standardOutput.toString()
            if (log.contains("connection success")) {
                logger.info(log)
                logger.lifecycle("\n* success * ${summary()}")
            } else {
                logger.lifecycle(log)
                logger.lifecycle("\n* failed * ${summary()}")
            }

            logger.lifecycle("" +
                    "\nThank you for testing, please leave some feedback at https://github.com/gesellix/docker-client/issues/26" +
                    "\nIf possible, please also share the log output above!")
        }
    }
})
