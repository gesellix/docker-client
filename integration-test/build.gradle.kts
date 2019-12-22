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
    implementation(project(":client"))
    implementation("com.kohlschutter.junixsocket:junixsocket-core:2.2.1")
    implementation("com.kohlschutter.junixsocket:junixsocket-common:2.2.1")

    testImplementation("net.jodah:failsafe:2.3.1")

    implementation("org.slf4j:slf4j-api:1.7.29")
    runtimeOnly("ch.qos.logback:logback-classic:1.2.3")

    testImplementation("de.gesellix:testutil:2019-12-21T20-15-14")
    testImplementation("org.spockframework:spock-core:1.3-groovy-2.5")
    testImplementation("cglib:cglib-nodep:3.3.0")
    testImplementation("org.apache.commons:commons-lang3:3.9")
    testImplementation("org.apache.commons:commons-compress:1.19")
    testImplementation("joda-time:joda-time:2.10.5")
    testImplementation("ch.qos.logback:logback-classic:1.2.3")
}
tasks.check.get().shouldRunAfter(project(":client").tasks.check)
