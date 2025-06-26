rootProject.name = "docker-client"
include(
    "client",
    "client-groovy4",
    "integration-test",
    "explore")

// https://docs.gradle.org/current/userguide/toolchains.html#sub:download_repositories
plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
