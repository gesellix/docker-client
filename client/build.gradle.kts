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
        strictly("[2023-04-01T01-01-01,)")
      }
    }
    implementation("de.gesellix:docker-filesocket") {
      version {
        strictly("[2023-04-01T01-01-01,)")
      }
    }
    implementation("de.gesellix:docker-remote-api-model-1-41") {
      version {
        strictly("[2023-04-01T01-01-01,)")
      }
    }
    implementation("org.slf4j:slf4j-api") {
      version {
        strictly("[1.7,3)")
        prefer("2.0.7")
      }
    }
    implementation("com.squareup.okhttp3:mockwebserver") {
      version {
        strictly("[4,5)")
        prefer("4.11.0")
      }
    }
    api("com.squareup.okhttp3:okhttp") {
      version {
        strictly("[4,5)")
        prefer("4.11.0")
      }
    }
    listOf(
      "com.squareup.okio:okio",
      "com.squareup.okio:okio-jvm"
    ).onEach {
      implementation(it) {
        version {
          strictly("[3,4)")
          prefer("3.3.0")
        }
      }
    }
    listOf(
      "org.jetbrains.kotlin:kotlin-reflect",
      "org.jetbrains.kotlin:kotlin-stdlib",
      "org.jetbrains.kotlin:kotlin-stdlib-jdk7",
      "org.jetbrains.kotlin:kotlin-stdlib-jdk8",
      "org.jetbrains.kotlin:kotlin-stdlib-common",
      "org.jetbrains.kotlin:kotlin-test"
    ).onEach {
      implementation(it) {
        version {
          strictly("[1.5,1.9)")
          prefer("1.8.21")
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
          prefer("3.0.17")
        }
      }
    }
    listOf(
      "com.squareup.moshi:moshi",
      "com.squareup.moshi:moshi-kotlin"
    ).onEach {
      implementation(it) {
        version {
          strictly("[1.12.0,)")
          prefer("1.14.0")
        }
      }
    }
  }

  // TODO consider changing this from api to implementation.
  // The change would require to move api.core client classes like `ClientException` to another module.
  api("de.gesellix:docker-remote-api-client:2023-04-28T13-53-00")
  api("de.gesellix:docker-remote-api-model-1-41:2023-04-28T10-46-00")
  api("de.gesellix:docker-engine:2023-04-30T11-10-00")
  api("de.gesellix:docker-compose:2023-04-28T10-37-00")

  implementation("org.codehaus.groovy:groovy:3.0.17")
  implementation("org.codehaus.groovy:groovy-json:3.0.17")

  api("com.squareup.moshi:moshi:1.14.0")
  implementation("com.google.re2j:re2j:1.7")

  implementation("org.slf4j:slf4j-api:2.0.7")
  testImplementation("ch.qos.logback:logback-classic:[1.2,2)!!1.3.6")

  implementation("com.squareup.okio:okio-jvm:3.3.0")
  api("com.squareup.okhttp3:okhttp:4.11.0")
  testImplementation("com.squareup.okhttp3:mockwebserver:[4,5)")

  implementation("org.apache.commons:commons-compress:1.23.0")

  implementation("org.bouncycastle:bcpkix-jdk15on:1.70")

  testImplementation("de.gesellix:testutil:[2022-12-01T01-01-01,)")

  testImplementation("org.junit.platform:junit-platform-launcher:1.9.3")
  testImplementation("org.spockframework:spock-core:2.3-groovy-3.0")
  testRuntimeOnly("net.bytebuddy:byte-buddy:1.14.4")
  testRuntimeOnly("org.objenesis:objenesis:3.3")
  testImplementation("io.github.joke:spock-mockable:2.3.0")

  testImplementation("org.apache.commons:commons-lang3:3.12.0")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(8))
  }
}

tasks {
  withType<JavaCompile> {
    options.encoding = "UTF-8"
  }

  withType<Test> {
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
