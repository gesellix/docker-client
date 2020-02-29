import com.jfrog.bintray.gradle.BintrayExtension
import org.gradle.api.internal.plugins.DslObject

buildscript {
    repositories {
        mavenLocal()
        jcenter()
        mavenCentral()
    }
}

project.extra.set("bintrayDryRun", false)

plugins {
    groovy
    `java-library`
    maven
    `maven-publish`
    id("com.github.ben-manes.versions")
    id("net.ossindex.audit")
    id("com.jfrog.bintray")
    id("io.freefair.github.package-registry-maven-publish")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenLocal()
    jcenter()
    mavenCentral()
}

dependencies {
    api("de.gesellix:docker-engine:2019-12-22T20-12-17")
    api("de.gesellix:docker-compose:2019-12-18T15-25-08")

    implementation("org.codehaus.groovy:groovy:2.5.8")
    implementation("org.codehaus.groovy:groovy-json:2.5.8")

    implementation("com.squareup.moshi:moshi:1.9.2")
    implementation("com.google.re2j:re2j:1.3")

    implementation("org.slf4j:slf4j-api:1.7.29")
    testImplementation("ch.qos.logback:logback-classic:1.2.3")

    api("com.squareup.okhttp3:okhttp:4.2.2")
    implementation("com.squareup.okio:okio:2.4.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.2.2")

    implementation("org.apache.commons:commons-compress:1.19")

    implementation("org.bouncycastle:bcpkix-jdk15on:1.64")

    testImplementation("de.gesellix:testutil:2019-12-21T20-15-14")

    testImplementation("org.spockframework:spock-core:1.3-groovy-2.5")
    testImplementation("cglib:cglib-nodep:3.3.0")

    testImplementation("org.apache.commons:commons-lang3:3.9")
}

tasks {
    withType(Test::class.java) {
        useJUnit()

        // minimal way of providing a special environment variable for the EnvFileParserTest
        environment("A_KNOWN_VARIABLE", "my value")
    }

    bintrayUpload {
        dependsOn("build")
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    dependsOn("classes")
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

artifacts {
    add("archives", sourcesJar.get())
}

tasks.install {
    DslObject(repositories)
            .convention
            .getPlugin<MavenRepositoryHandlerConvention>()
            .mavenInstaller {
                pom {
                    groupId = "de.gesellix"
                    artifactId = "docker-client"
                    version = "local"
                }
            }
}

val publicationName = "dockerClient"
publishing {
    publications {
        register(publicationName, MavenPublication::class) {
            groupId = "de.gesellix"
            artifactId = "docker-client"
            version = rootProject.extra["artifactVersion"] as String
            from(components["java"])
            artifact(sourcesJar.get())
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
        }
    }
}

fun findProperty(s: String) = project.findProperty(s) as String?

bintray {
    user = System.getenv()["BINTRAY_USER"] ?: findProperty("bintray.user")
    key = System.getenv()["BINTRAY_API_KEY"] ?: findProperty("bintray.key")
    setPublications(publicationName)
    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "docker-utils"
        name = "docker-client"
        desc = "A Docker client for the JVM written in Groovy"
        setLicenses("Apache-2.0")
        setLabels("docker", "engine api", "remote api", "client", "java", "groovy")
        version.name = rootProject.extra["artifactVersion"] as String
        vcsUrl = "https://github.com/gesellix/docker-client.git"
    })
    dryRun = project.extra["bintrayDryRun"] as Boolean
}
