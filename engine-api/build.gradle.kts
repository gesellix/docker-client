plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.kotlin.kapt")
  id("com.github.ben-manes.versions")
}

version = "1.41"

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.31")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
  implementation("com.squareup.moshi:moshi:1.12.0")
//  implementation("com.squareup.moshi:moshi-kotlin:1.12.0")
  kapt("com.squareup.moshi:moshi-kotlin-codegen:1.12.0")
  implementation("com.squareup.okhttp3:okhttp:4.9.1")
  testImplementation("io.kotlintest:kotlintest-runner-junit5:3.4.2")
  implementation("de.gesellix:docker-remote-api-model-1-41:2021-10-04T21-50-00")
  implementation("de.gesellix:docker-engine:2021-09-29T15-30-00")
  implementation("de.gesellix:docker-filesocket:2021-09-20T20-10-00")

  implementation("org.slf4j:slf4j-api:[1.7,)!!1.7.32")
  testImplementation("ch.qos.logback:logback-classic:[1.2,2)!!1.2.6")

//  implementation("com.squareup.okhttp3:logging-interceptor:4.9.1")
//  implementation("org.apache.commons:commons-lang3:3.10")
//  implementation("javax.annotation:javax.annotation-api:1.3.2")
//  testImplementation("junit:junit:4.13.1")
}

tasks.withType(Test::class.java) {
  useJUnitPlatform()
}

//tasks.javadoc {
//  options.tags = ["http.response.details:a:Http Response Details"]
//}
