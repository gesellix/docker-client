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
    implementation("org.codehaus.groovy:groovy:2.5.9")
    testImplementation("org.apache.commons:commons-compress:1.20")

    implementation("org.slf4j:slf4j-api:1.7.30")
    runtimeOnly("ch.qos.logback:logback-classic:1.2.3")

    testImplementation("org.spockframework:spock-core:1.3-groovy-2.5")
    testRuntimeOnly("cglib:cglib-nodep:3.3.0")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.2.3")
}
