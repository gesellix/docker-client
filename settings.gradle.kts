rootProject.name = "docker-client"
include(
    "client",
    "client-groovy4",
    "integration-test",
    "explore")

// https://docs.gradle.org/8.0.1/userguide/toolchains.html#sub:download_repositories
plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version("0.4.0")
}
