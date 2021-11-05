plugins {
  id("maven-publish")
  id("com.github.ben-manes.versions") version "0.39.0"
  id("net.ossindex.audit") version "0.4.11"
  id("io.freefair.maven-central.validate-poms") version "6.2.0"
  id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
  id("org.jetbrains.kotlin.jvm") version "1.5.31" apply false
  id("org.jetbrains.kotlin.kapt") version "1.5.31" apply false
}

val dependencyVersions = listOf(
  "com.fasterxml.jackson.core:jackson-databind:2.9.10.8",
  "com.squareup.moshi:moshi:1.12.0",
  "com.squareup.okio:okio:2.10.0",
  "org.apache.commons:commons-lang3:3.12.0",
  "org.apiguardian:apiguardian-api:1.1.0",
  "org.codehaus.groovy:groovy:3.0.9",
  "org.codehaus.groovy:groovy-json:3.0.9",
  "org.jetbrains:annotations:22.0.0",
  "org.jetbrains.kotlin:kotlin-reflect:1.5.31",
  "org.jetbrains.kotlin:kotlin-stdlib:1.5.31",
  "org.jetbrains.kotlin:kotlin-stdlib-common:1.5.31",
  "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.5.31",
  "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.31",
  "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2",
  "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.5.2",
  "org.junit:junit-bom:5.8.1",
  "org.junit.jupiter:junit-jupiter-api:5.8.1",
  "org.junit.platform:junit-platform-commons:1.8.0",
  "org.junit.platform:junit-platform-engine:1.8.0",
  "org.junit.platform:junit-platform-launcher:1.8.0",
  "org.junit.platform:junit-platform-suite-api:1.8.0",
  "org.opentest4j:opentest4j:1.2.0"
)

val dependencyVersionsByGroup = mapOf<String, String>()

allprojects {
  configurations.all {
    resolutionStrategy {
      failOnVersionConflict()
      force(dependencyVersions)
      eachDependency {
        val forcedVersion = dependencyVersionsByGroup[requested.group]
        if (forcedVersion != null) {
          useVersion(forcedVersion)
        }
      }
    }
  }
}

subprojects {
  repositories {
//    mavenLocal()
//    fun findProperty(s: String) = project.findProperty(s) as String?
//    listOf(
//      "docker-client/*"
//    ).forEach { repo ->
//      maven {
//        name = "github"
//        setUrl("https://maven.pkg.github.com/$repo")
//        credentials {
//          username = System.getenv("PACKAGE_REGISTRY_USER") ?: findProperty("github.package-registry.username")
//          password = System.getenv("PACKAGE_REGISTRY_TOKEN") ?: findProperty("github.package-registry.password")
//        }
//      }
//    }
    mavenCentral()
  }
}

allprojects {
  configurations.all {
    resolutionStrategy {
      failOnVersionConflict()
      force(dependencyVersions)
      eachDependency {
        val forcedVersion = dependencyVersionsByGroup[requested.group]
        if (forcedVersion != null) {
          useVersion(forcedVersion)
        }
      }
    }
  }
}

fun findProperty(s: String) = project.findProperty(s) as String?

val isSnapshot = project.version == "unspecified"
nexusPublishing {
  repositories {
    if (!isSnapshot) {
      sonatype {
        // 'sonatype' is pre-configured for Sonatype Nexus (OSSRH) which is used for The Central Repository
        stagingProfileId.set(System.getenv("SONATYPE_STAGING_PROFILE_ID") ?: findProperty("sonatype.staging.profile.id")) //can reduce execution time by even 10 seconds
        username.set(System.getenv("SONATYPE_USERNAME") ?: findProperty("sonatype.username"))
        password.set(System.getenv("SONATYPE_PASSWORD") ?: findProperty("sonatype.password"))
      }
    }
  }
}

project.apply("debug.gradle.kts")
