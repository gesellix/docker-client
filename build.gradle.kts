plugins {
  id("maven-publish")
  id("com.github.ben-manes.versions") version "0.39.0"
  id("net.ossindex.audit") version "0.4.11"
  id("io.freefair.maven-central.validate-poms") version "5.3.3.3"
  id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

val dependencyVersions = listOf(
  "junit:junit:4.13.2",
  "org.codehaus.groovy:groovy:2.5.13",
  "org.codehaus.groovy:groovy-json:2.5.13",
  "org.codehaus.groovy:groovy-macro:2.5.13",
  "org.codehaus.groovy:groovy-nio:2.5.13",
  "org.codehaus.groovy:groovy-sql:2.5.13",
  "org.codehaus.groovy:groovy-templates:2.5.13",
  "org.codehaus.groovy:groovy-test:2.5.13",
  "org.codehaus.groovy:groovy-xml:2.5.13"
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
