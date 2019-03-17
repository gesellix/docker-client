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
    runtime("ch.qos.logback:logback-classic:1.2.3")

    testCompile("org.spockframework:spock-core:1.3-groovy-2.5")
    testCompile("cglib:cglib-nodep:3.2.10")
    testCompile("ch.qos.logback:logback-classic:1.2.3")
}
