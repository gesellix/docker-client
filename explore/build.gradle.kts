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
    listOf(libs.bundles.okhttp).forEach {
      implementation(it) {
        version {
          strictly(libs.versions.okhttpVersionrange.get())
          prefer(libs.versions.okhttp.get())
        }
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
    listOf(libs.bundles.groovy3).forEach {
      implementation(it) {
        version {
          strictly(libs.versions.groovy3Versionrange.get())
          prefer(libs.versions.groovy3.get())
        }
      }
    }
    listOf(libs.bundles.groovy4).forEach {
      implementation(it) {
        version {
          strictly(libs.versions.groovy4Versionrange.get())
          prefer(libs.versions.groovy4.get())
        }
      }
    }
  }
//  implementation(project(":client"))
  implementation(project(":client-groovy4"))
//  implementation(libs.groovy3)
  implementation(libs.groovy4)
  testImplementation("org.apache.commons:commons-compress:1.28.0")
  implementation("de.gesellix:docker-remote-api-model-1-41:2025-10-31T17-49-00")
  implementation("de.gesellix:docker-filesocket:2025-10-31T17-48-00")

  implementation(libs.slf4j)
  runtimeOnly("ch.qos.logback:logback-classic:${libs.versions.logbackVersionrange.get()}!!${libs.versions.logback.get()}")

//  testImplementation("org.spockframework:spock-core:2.3-groovy-3.0")
  testImplementation("org.spockframework:spock-core:2.3-groovy-4.0")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
  testRuntimeOnly("net.bytebuddy:byte-buddy:1.18.1")
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
