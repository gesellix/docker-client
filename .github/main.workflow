workflow "ci" {
  on = "push"
  resolves = ["java"]
}

action "java" {
  uses = "MrRamych/gradle-actions/openjdk-12@2.1"
  args = "clean build"
  runs = "./gradlew"
}
