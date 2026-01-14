plugins {
  id("maven-publish")
  id("com.github.ben-manes.versions") version "0.53.0"
  id("org.sonatype.gradle.plugins.scan") version "3.1.4"
  id("io.freefair.maven-central.validate-poms") version "9.2.0"
  id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

val dependencyVersions = listOf(
  "net.bytebuddy:byte-buddy:1.18.3",
  "net.bytebuddy:byte-buddy-agent:1.18.3",
  "org.apache.commons:commons-compress:1.28.0",
  "org.apache.commons:commons-lang3:3.20.0",
  "org.bouncycastle:bcpkix-jdk18on:1.83",
  "org.codehaus.groovy:groovy:3.0.23",
  "org.codehaus.groovy:groovy-json:3.0.23",
  "org.apache.groovy:groovy:4.0.24",
  "org.apache.groovy:groovy-json:4.0.24",
  "org.jetbrains:annotations:26.0.2-1",
  "org.junit:junit-bom:5.13.4",
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
            .using(module("org.bouncycastle:bcpkix-jdk18on:1.83"))
      }
    }
  }
}

ossIndexAudit {
  username = System.getenv("SONATYPE_INDEX_USERNAME") ?: findProperty("sonatype.index.username")
  password = System.getenv("SONATYPE_INDEX_PASSWORD") ?: findProperty("sonatype.index.password")
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
        nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
        snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
      }
    }
  }
}

//project.apply("debug.gradle.kts")
