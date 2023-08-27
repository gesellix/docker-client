plugins {
  id("maven-publish")
  id("com.github.ben-manes.versions") version "0.47.0"
  id("net.ossindex.audit") version "0.4.11"
  id("io.freefair.maven-central.validate-poms") version "8.2.2"
  id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
}

val dependencyVersions = listOf(
  "com.squareup.okio:okio-jvm:3.5.0",
  "net.bytebuddy:byte-buddy:1.14.6",
  "net.bytebuddy:byte-buddy-agent:1.14.6",
  "org.apache.commons:commons-compress:1.23.0",
  "org.codehaus.groovy:groovy:3.0.19",
  "org.jetbrains:annotations:24.0.1",
  "org.junit:junit-bom:5.10.0",
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
      // https://docs.gradle.org/current/userguide/resolution_rules.html
      dependencySubstitution {
        substitute(module("org.bouncycastle:bcpkix-jdk15on"))
            .using(module("org.bouncycastle:bcpkix-jdk18on:1.76"))
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

//project.apply("debug.gradle.kts")
