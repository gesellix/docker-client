workflow "ci" {
  on = "push"
  resolves = ["clean build"]
}

action "clean build" {
  uses = "MrRamych/gradle-actions/openjdk-12@2.1"
  args = "clean build"
}
