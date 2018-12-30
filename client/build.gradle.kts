import com.jfrog.bintray.gradle.BintrayExtension
import org.gradle.api.internal.plugins.DslObject

buildscript {
    repositories {
        mavenLocal()
        jcenter()
        mavenCentral()
    }

//    dependencies {
//        classpath "net.saliman:gradle-cobertura-plugin:2.4.0"
//        classpath "org.kt3k.gradle.plugin:coveralls-gradle-plugin:2.8.1"
//    }
}

project.extra.set("bintrayDryRun", false)

plugins {
    groovy
    maven
    `maven-publish`
    id("com.github.ben-manes.versions")
    id("net.ossindex.audit")
    id("com.jfrog.bintray")
}

//apply plugin: "jacoco"
//apply plugin: "net.saliman.cobertura"
//apply plugin: "com.github.kt3k.coveralls"

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
    //    compile("de.gesellix:docker-engine:local")
    compile("de.gesellix:docker-engine:2018-12-30T13-44-59")
    compile("de.gesellix:docker-compose:2018-12-29T20-53-51")

    compile("org.codehaus.groovy:groovy:2.5.4")
    compile("org.codehaus.groovy:groovy-json:2.5.4")

    compile("org.slf4j:slf4j-api:1.7.25")
    testCompile("ch.qos.logback:logback-classic:1.2.3")

    compile("com.squareup.okio:okio:2.1.0")
    compile("com.squareup.okhttp3:okhttp:3.12.1")
    testCompile("com.squareup.okhttp3:mockwebserver:3.12.1")

    compile("org.apache.commons:commons-compress:1.18")

    compile("org.bouncycastle:bcpkix-jdk15on:1.60")

    testCompile("de.gesellix:testutil:2018-12-29T16-12-32")

    testCompile("org.spockframework:spock-core:1.2-groovy-2.5")
    testCompile("cglib:cglib-nodep:3.2.10")

    testCompile("org.apache.commons:commons-lang3:3.8.1")
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

//cobertura {
//    coverageSourceDirs = sourceSets.main.groovy.srcDirs
//    coverageFormats = ["html", "xml"]
//}

//jacocoTestReport.reports {
//    xml.enabled = true
//    html.enabled = true
//}

val sourcesJar by tasks.registering(Jar::class) {
    dependsOn("classes")
    classifier = "sources"
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
