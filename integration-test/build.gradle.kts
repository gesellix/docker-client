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

    testCompile("de.gesellix:testutil:2019-04-03T07-51-41")
    testCompile("org.spockframework:spock-core:1.3-groovy-2.5")
    testCompile("cglib:cglib-nodep:3.2.10")
    testCompile("org.apache.commons:commons-lang3:3.8.1")
    testCompile("joda-time:joda-time:2.10.1")
    testCompile("ch.qos.logback:logback-classic:1.2.3")
}
tasks.check.get().shouldRunAfter(project(":client").tasks.check)
