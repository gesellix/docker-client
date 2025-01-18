plugins {
  groovy
  id("com.github.ben-manes.versions")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(8))
  }
}

dependencies {
  constraints {
    implementation(libs.slf4j) {
      version {
        strictly(libs.versions.slf4jVersionrange.get())
        prefer(libs.versions.slf4j.get())
      }
    }
    implementation(libs.okio) {
      version {
        strictly(libs.versions.okioVersionrange.get())
      }
    }
    listOf(libs.bundles.kotlin).forEach {
      implementation(it) {
        version {
          strictly(libs.versions.kotlinVersionrange.get())
          prefer(libs.versions.kotlin.get())
        }
      }
    }
    listOf(
        "org.codehaus.groovy:groovy",
        "org.codehaus.groovy:groovy-json"
    ).forEach {
      implementation(it) {
        version {
          strictly("[3,4)")
          prefer("3.0.23")
        }
      }
    }
    listOf(
      "org.apache.groovy:groovy",
      "org.apache.groovy:groovy-json"
    ).forEach {
      implementation(it) {
        version {
          strictly("[4,)")
          prefer("4.0.24")
        }
      }
    }
  }
//  implementation(project(":client"))
  implementation(project(":client-groovy4"))
//  implementation("org.codehaus.groovy:groovy:[3,4)")
  implementation("org.apache.groovy:groovy:4.0.24")
  testImplementation("org.apache.commons:commons-compress:1.27.1")

  implementation(libs.slf4j)
  runtimeOnly("ch.qos.logback:logback-classic:${libs.versions.logbackVersionrange.get()}!!${libs.versions.logback.get()}")

//  testImplementation("org.spockframework:spock-core:2.3-groovy-3.0")
  testImplementation("org.spockframework:spock-core:2.3-groovy-4.0")
  testRuntimeOnly("net.bytebuddy:byte-buddy:1.16.0")
  testRuntimeOnly("ch.qos.logback:logback-classic:${libs.versions.logbackVersionrange.get()}!!${libs.versions.logback.get()}")
}

tasks {
  withType<JavaCompile> {
    options.encoding = "UTF-8"
  }
  withType<Test> {
    useJUnitPlatform()
  }
}
