import java.text.SimpleDateFormat
import java.util.*

plugins {
  id("groovy")
  id("java-library")
  id("maven-publish")
  id("signing")
  id("com.github.ben-manes.versions")
  id("net.ossindex.audit")
  id("io.freefair.maven-central.validate-poms")
}

dependencies {
  constraints {
    implementation("de.gesellix:docker-engine") {
      version {
        strictly("[2022-02-01T01-01-01,)")
      }
    }
    implementation("de.gesellix:docker-filesocket") {
      version {
        strictly("[2022-02-01T01-01-01,)")
      }
    }
    implementation("de.gesellix:docker-remote-api-model-1-41") {
      version {
        strictly("[2022-02-01T01-01-01,)")
      }
    }
    implementation("org.slf4j:slf4j-api") {
      version {
        strictly("[1.7,1.8)")
        prefer("1.7.36")
      }
    }
    implementation("com.squareup.okhttp3:mockwebserver") {
      version {
        strictly("[4,5)")
        prefer("4.9.3")
      }
    }
    api("com.squareup.okhttp3:okhttp") {
      version {
        strictly("[4,5)")
        prefer("4.9.3")
      }
    }
    listOf(
      "com.squareup.okio:okio",
      "com.squareup.okio:okio-jvm"
    ).onEach {
      implementation(it) {
        version {
          strictly("[3,4)")
          prefer("3.1.0")
        }
      }
    }
    listOf(
      "org.jetbrains.kotlin:kotlin-reflect",
      "org.jetbrains.kotlin:kotlin-stdlib",
      "org.jetbrains.kotlin:kotlin-stdlib-jdk8",
      "org.jetbrains.kotlin:kotlin-stdlib-common",
      "org.jetbrains.kotlin:kotlin-test"
    ).onEach {
      implementation(it) {
        version {
          strictly("[1.5,1.7)")
          prefer("1.6.21")
        }
      }
    }
    listOf(
      "org.codehaus.groovy:groovy",
      "org.codehaus.groovy:groovy-json"
    ).onEach {
      implementation(it) {
        version {
          strictly("[3,)")
          prefer("3.0.10")
        }
      }
    }
    implementation("com.squareup.moshi:moshi") {
      version {
        strictly("[1.12.0,)")
        prefer("1.13.0")
      }
    }
  }

  // TODO consider changing this from api to implementation.
  // The change would require to move api.core client classes like `ClientException` to another module.
  api("de.gesellix:docker-remote-api-client:2022-05-24T00-30-00")
  api("de.gesellix:docker-remote-api-model-1-41:2022-05-23T21-51-00")
  api("de.gesellix:docker-engine:2022-05-03T22-53-00")
  api("de.gesellix:docker-compose:2022-05-23T11-46-00")

  implementation("org.codehaus.groovy:groovy:3.0.10")
  implementation("org.codehaus.groovy:groovy-json:3.0.11")

  api("com.squareup.moshi:moshi:1.13.0")
  implementation("com.google.re2j:re2j:1.6")

  implementation("org.slf4j:slf4j-api:1.7.36")
  testImplementation("ch.qos.logback:logback-classic:[1.2,2)!!1.2.11")

  implementation("com.squareup.okio:okio-jvm:3.1.0")
  api("com.squareup.okhttp3:okhttp:4.9.3")
  testImplementation("com.squareup.okhttp3:mockwebserver:[4,5)")

  implementation("org.apache.commons:commons-compress:1.21")

  implementation("org.bouncycastle:bcpkix-jdk15on:1.70")

  testImplementation("de.gesellix:testutil:[2021-08-05T22-09-32,)")

  testImplementation("org.spockframework:spock-core:2.1-groovy-3.0")
  testRuntimeOnly("cglib:cglib-nodep:3.3.0")
  testImplementation("io.github.joke:spock-mockable:1.5.5")

  testImplementation("org.apache.commons:commons-lang3:3.12.0")
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
  withType(Test::class.java) {
    useJUnitPlatform()

    // minimal way of providing a special environment variable for the EnvFileParserTest
    environment("A_KNOWN_VARIABLE", "my value")
  }
}

val javadocJar by tasks.registering(Jar::class) {
  dependsOn("classes")
  archiveClassifier.set("javadoc")
  from(tasks.javadoc)
}

val sourcesJar by tasks.registering(Jar::class) {
  dependsOn("classes")
  archiveClassifier.set("sources")
  from(sourceSets.main.get().allSource)
}

artifacts {
  add("archives", sourcesJar.get())
  add("archives", javadocJar.get())
}

fun findProperty(s: String) = project.findProperty(s) as String?

val isSnapshot = project.version == "unspecified"
val artifactVersion = if (!isSnapshot) project.version as String else SimpleDateFormat("yyyy-MM-dd\'T\'HH-mm-ss").format(Date())!!
val publicationName = "dockerClient"
publishing {
  repositories {
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/${property("github.package-registry.owner")}/${property("github.package-registry.repository")}")
      credentials {
        username = System.getenv("GITHUB_ACTOR") ?: findProperty("github.package-registry.username")
        password = System.getenv("GITHUB_TOKEN") ?: findProperty("github.package-registry.password")
      }
    }
  }
  publications {
    register(publicationName, MavenPublication::class) {
      pom {
        name.set("docker-client")
        description.set("A Docker client for the JVM written in Groovy")
        url.set("https://github.com/gesellix/docker-client")
        licenses {
          license {
            name.set("MIT")
            url.set("https://opensource.org/licenses/MIT")
          }
        }
        developers {
          developer {
            id.set("gesellix")
            name.set("Tobias Gesellchen")
            email.set("tobias@gesellix.de")
          }
        }
        scm {
          connection.set("scm:git:github.com/gesellix/docker-client.git")
          developerConnection.set("scm:git:ssh://github.com/gesellix/docker-client.git")
          url.set("https://github.com/gesellix/docker-client")
        }
      }
      artifactId = "docker-client"
      version = artifactVersion
      from(components["java"])
      artifact(sourcesJar.get())
      artifact(javadocJar.get())
    }
  }
}

signing {
  val signingKey: String? by project
  val signingPassword: String? by project
  useInMemoryPgpKeys(signingKey, signingPassword)
  sign(publishing.publications[publicationName])
}
