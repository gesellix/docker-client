buildscript {
    repositories {
        mavenLocal()
        jcenter()
        mavenCentral()
    }
}

plugins {
    groovy
    id("com.github.ben-manes.versions")
}

repositories {
    mavenLocal()
    jcenter()
    mavenCentral()
}

dependencies {
    constraints {
        implementation("org.slf4j:slf4j-api") {
            version {
                strictly("1.7.30")
            }
        }
        listOf("org.codehaus.groovy:groovy",
                "org.codehaus.groovy:groovy-json").onEach {
            implementation(it) {
                version {
                    strictly("2.5.9")
                }
            }
        }
    }
    implementation(project(":client"))
    testImplementation("com.kohlschutter.junixsocket:junixsocket-core:2.3.2")
    testImplementation("com.kohlschutter.junixsocket:junixsocket-common:2.3.2")

    testImplementation("net.jodah:failsafe:2.3.4")
    testImplementation("org.apache.commons:commons-compress:1.20")

    testImplementation("org.slf4j:slf4j-api:1.7.30")
    runtimeOnly("ch.qos.logback:logback-classic:1.2.3")

    testImplementation("de.gesellix:testutil:2020-03-29T11-41-48")
    testImplementation("org.spockframework:spock-core:1.3-groovy-2.5")
    testRuntimeOnly("cglib:cglib-nodep:3.3.0")
    testImplementation("org.apache.commons:commons-lang3:3.10")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.2.3")
}
tasks.check.get().shouldRunAfter(project(":client").tasks.check)
