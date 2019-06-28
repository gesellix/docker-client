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
    compile(project(":client"))

    testCompile("net.jodah:failsafe:2.0.1")

    runtime("ch.qos.logback:logback-classic:1.2.3")

    testCompile("de.gesellix:testutil:2019-06-27T19-49-48")
    testCompile("org.spockframework:spock-core:1.3-groovy-2.5")
    testCompile("cglib:cglib-nodep:3.2.12")
    testCompile("org.apache.commons:commons-lang3:3.9")
    testCompile("joda-time:joda-time:2.10.2")
    testCompile("ch.qos.logback:logback-classic:1.2.3")
}
tasks.check.get().shouldRunAfter(project(":client").tasks.check)
