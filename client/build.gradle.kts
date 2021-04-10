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

repositories {
  mavenCentral()
}

dependencies {
  constraints {
    implementation("org.slf4j:slf4j-api") {
      version {
        strictly("[1.7,1.8)")
        prefer("1.7.30")
      }
    }
    implementation("com.squareup.okhttp3:mockwebserver") {
      version {
        strictly("[4,5)")
        prefer("4.9.1")
      }
    }
    api("com.squareup.okhttp3:okhttp") {
      version {
        strictly("[4,5)")
        prefer("4.9.1")
      }
    }
    implementation("com.squareup.okio:okio") {
      version {
        strictly("[2.5,3)")
        prefer("2.10.0")
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
          strictly("[1.3,1.5)")
          prefer("1.3.72")
        }
      }
    }
    listOf(
      "org.codehaus.groovy:groovy",
      "org.codehaus.groovy:groovy-json"
    ).onEach {
      implementation(it) {
        version {
          strictly("[2.5,)")
          prefer("2.5.13")
        }
      }
    }
  }

  api("de.gesellix:docker-engine:2021-04-10T11-44-19")
  api("de.gesellix:docker-compose:2021-04-07T12-44-08")

  implementation("org.codehaus.groovy:groovy:[2.5,)")
  implementation("org.codehaus.groovy:groovy-json:[2.5,)")

  api("com.squareup.moshi:moshi:[1.9,2)")
  implementation("com.google.re2j:re2j:1.6")

  implementation("org.slf4j:slf4j-api:[1.7,)")
  //implementation("org.slf4j:jul-to-slf4j:1.7.30")
  testImplementation("ch.qos.logback:logback-classic:1.2.3")

  implementation("com.squareup.okio:okio:[2,)")
  api("com.squareup.okhttp3:okhttp:[4,5)")
  testImplementation("com.squareup.okhttp3:mockwebserver:[4,5)")

  implementation("org.apache.commons:commons-compress:1.20")

  implementation("org.bouncycastle:bcpkix-jdk15on:1.68")

  testImplementation("de.gesellix:testutil:[2020-10-03T10-08-28,)")

  testImplementation("org.spockframework:spock-core:1.3-groovy-2.5")
  testRuntimeOnly("cglib:cglib-nodep:3.3.0")

  testImplementation("org.apache.commons:commons-lang3:3.12.0")
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
  withType(Test::class.java) {
    useJUnit()

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
