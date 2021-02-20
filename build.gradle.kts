import java.text.SimpleDateFormat
import java.util.*

rootProject.extra.set("artifactVersion", SimpleDateFormat("yyyy-MM-dd\'T\'HH-mm-ss").format(Date()))

buildscript {
    repositories {
        mavenLocal()
        jcenter()
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("com.github.ben-manes.versions") version "0.33.0"
    id("com.jfrog.bintray") version "1.8.5" apply false
    id("net.ossindex.audit") version "0.4.11"
    id("io.freefair.github.package-registry-maven-publish") version "5.2.1" // apply false
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
        "org.codehaus.groovy:groovy-xml:2.5.13"
)

subprojects {
    configurations.all {
        resolutionStrategy {
            failOnVersionConflict()
            force(dependencyVersions)
        }
    }
}

fun findProperty(s: String) = project.findProperty(s) as String?

rootProject.github {
    slug.set("${project.property("github.package-registry.owner")}/${project.property("github.package-registry.repository")}")
    username.set(System.getenv("GITHUB_ACTOR") ?: findProperty("github.package-registry.username"))
    token.set(System.getenv("GITHUB_TOKEN") ?: findProperty("github.package-registry.password"))
}

tasks {
  wrapper {
    gradleVersion = "6.8.2"
    distributionType = Wrapper.DistributionType.ALL
  }
}

project.apply("debug.gradle.kts")
