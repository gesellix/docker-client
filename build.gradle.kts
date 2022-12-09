plugins {
  id("maven-publish")
  id("com.github.ben-manes.versions") version "0.44.0"
  id("net.ossindex.audit") version "0.4.11"
  id("io.freefair.maven-central.validate-poms") version "6.6.1"
  id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

val dependencyVersions = listOf(
  "com.squareup.okio:okio-jvm:3.2.0",
  "net.bytebuddy:byte-buddy:1.12.19",
  "net.bytebuddy:byte-buddy-agent:1.12.19",
  "org.apache.commons:commons-compress:1.22",
  "org.codehaus.groovy:groovy:3.0.13",
  "org.jetbrains:annotations:23.1.0",
  "org.junit:junit-bom:5.9.1",
)

val dependencyVersionsByGroup = mapOf<String, String>()

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
