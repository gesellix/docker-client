plugins {
  groovy
  id("com.github.ben-manes.versions")
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  constraints {
    implementation("org.slf4j:slf4j-api") {
      version {
        strictly("[1.7,1.8)")
        prefer("1.7.36")
      }
    }
    implementation("com.squareup.okio:okio") {
      version {
        strictly("[3,4)")
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
          strictly("[1.5,1.8)")
          prefer("1.6.21")
        }
      }
    }
    listOf(
      "org.codehaus.groovy:groovy",
      "org.codehaus.groovy:groovy-json"
    ).onEach {
      implementation(it) {
        version {
          strictly("[3,)")
          prefer("3.0.12")
        }
      }
    }
  }
  implementation(project(":client"))
  implementation("org.codehaus.groovy:groovy:3.0.12")
  testImplementation("org.apache.commons:commons-compress:1.21")

  implementation("org.slf4j:slf4j-api:2.0.0")
  runtimeOnly("ch.qos.logback:logback-classic:[1.2,2)!!1.2.11")

  testImplementation("org.spockframework:spock-core:2.1-groovy-3.0")
  testRuntimeOnly("cglib:cglib-nodep:3.3.0")
  testRuntimeOnly("ch.qos.logback:logback-classic:[1.2,2)!!1.2.11")
}

tasks.withType(Test::class) {
  useJUnitPlatform()
}
