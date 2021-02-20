import java.util.*

buildscript {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
}

plugins {
  id("maven-publish")
  id("com.github.ben-manes.versions") version "0.36.0"
  id("net.ossindex.audit") version "0.4.11"
  id("io.freefair.maven-central.validate-poms") version "5.3.0"
  id("io.github.gradle-nexus.publish-plugin") version "1.0.0"
}

val dependencyVersions = listOf(
  "junit:junit:4.13",
  "org.codehaus.groovy:groovy:2.5.13",
  "org.codehaus.groovy:groovy-json:2.5.13",
  "org.codehaus.groovy:groovy-macro:2.5.13",
  "org.codehaus.groovy:groovy-nio:2.5.13",
  "org.codehaus.groovy:groovy-sql:2.5.13",
  "org.codehaus.groovy:groovy-templates:2.5.13",
  "org.codehaus.groovy:groovy-test:2.5.13",
  "org.codehaus.groovy:groovy-xml:2.5.13",
  "org.squareup.moshi:moshi:1.11.0",
  "org.squareup.moshi:moshi-kotlin:1.11.0",
  "org.squareup.okhttp3:okhttp:4.9.0",
  "org.squareup.okio:okio:2.9.0"
)

val dependencyGroupVersions = mapOf<String, String>()

allprojects {
  configurations.all {
    resolutionStrategy {
      failOnVersionConflict()
      force(dependencyVersions)
      eachDependency {
        val forcedVersion = dependencyGroupVersions[requested.group]
        if (forcedVersion != null) {
          useVersion(forcedVersion)
        }
      }
    }
  }
}

fun findProperty(s: String) = project.findProperty(s) as String?

tasks {
  wrapper {
    gradleVersion = "6.8.2"
    distributionType = Wrapper.DistributionType.ALL
  }
}

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
