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
    implementation("org.codehaus.groovy:groovy:2.5.8")

    implementation("org.slf4j:slf4j-api:1.7.29")
    runtimeOnly("ch.qos.logback:logback-classic:1.2.3")

    testImplementation("org.spockframework:spock-core:1.3-groovy-2.5")
    testImplementation("cglib:cglib-nodep:3.3.0")
    testImplementation("ch.qos.logback:logback-classic:1.2.3")
}
