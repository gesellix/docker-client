workflow "ci" {
  on = "push"
  resolves = ["gradle"]
}

action "gradle" {
  uses = "gradle"
  args = "clean build"
}
