workflow "ci" {
  on = "push"
  resolves = ["java"]
}

action "java" {
  uses = "docker://gradle:12-jdk"
  args = "clean build"
  runs = "./gradlew"
}
