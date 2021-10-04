plugins {
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.kotlin.kapt")
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
        prefer("1.7.32")
      }
    }
    implementation("com.squareup.okio:okio") {
      version {
        strictly("[2.5,3)")
        prefer("2.10.0")
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
          strictly("[1.3,1.6)")
          prefer("1.5.31")
        }
      }
    }
  }
  implementation(project(":engine-api"))
  implementation("de.gesellix:docker-engine:2021-09-29T15-30-00")
  implementation("de.gesellix:docker-remote-api-model-1-41:2021-10-04T21-50-00")
  implementation("com.squareup.moshi:moshi:1.12.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")

  implementation("org.slf4j:slf4j-api")
  testRuntimeOnly("ch.qos.logback:logback-classic:[1.2,2)!!1.2.6")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
  testRuntimeOnly("cglib:cglib-nodep:3.3.0")
  testImplementation("org.junit.platform:junit-platform-launcher:1.8.0")
  testImplementation("org.junit.platform:junit-platform-commons:1.8.0")
  testImplementation("org.apache.commons:commons-compress:1.21")
  testImplementation("de.gesellix:testutil:[2021-08-05T22-09-32,)")
}

tasks.withType(Test::class.java) {
  useJUnitPlatform()
}
