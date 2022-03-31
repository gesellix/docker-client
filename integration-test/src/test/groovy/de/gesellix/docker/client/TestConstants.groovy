package de.gesellix.docker.client

class TestConstants {

  final String imageRepo
  final String imageTag
  final String imageName
  final String imageDigest
  final String imageId
  final int imageCreated
  final String volumeTarget

  final Map<String, Closure<Boolean>> versionDetails = [:]

  static TestConstants CONSTANTS = new TestConstants()

  TestConstants() {
    if (LocalDocker.isNativeWindows()) {
      imageRepo = "gesellix/echo-server"
      imageTag = "os-windows"
      imageDigest = "gesellix/echo-server@sha256:e050aff7118d4b03762035a12e348445b3bea02a9966b84aa2d6c9759a9128e1"
      imageId = "sha256:8e0ebadb075bdf6fa010527da9af8fb66e4149a57a3867d6c54b1963fbff416d"
      imageCreated = 1648543890
      volumeTarget = "C:/my-volume"
    }
    else {
      imageRepo = "gesellix/echo-server"
      imageTag = "os-linux"
      imageDigest = "gesellix/echo-server@sha256:0c82ad1c87b9662383e3876d8d462cbdeadb078707c9a3bce548a4fababf19fe"
      imageId = "sha256:19b711b0cdc5f372fc3cdf3bfa7b68ebcbc10cae82f0925cca615919efd7c541"
      imageCreated = 1648543735
      volumeTarget = "/my-volume"
    }
    imageName = "$imageRepo:$imageTag"

    if (System.getenv("GITHUB_ACTOR")) {
      // TODO consider checking the Docker api version instead of "GITHUB_ACTOR"
      if (LocalDocker.isNativeWindows()) {
        versionDetails = [
            ApiVersion   : { it == "1.40" },
            Arch         : { it == "amd64" },
            BuildTime    : { it == "12/17/2020 19:29:00" },
            GitCommit    : { it == "57e3a05525" },
            GoVersion    : { it == "go1.13.15" },
            KernelVersion: { it =~ "\\d.\\d{1,2}.\\d{1,2}\\w*" },
            MinAPIVersion: { it == "1.24" },
            Os           : { it == "windows" },
            Version      : { it =~ "\\d{1,2}.\\d{1,2}.\\d{1,2}\\w*" }]
      }
      else {
        versionDetails = [
            ApiVersion   : { it == "1.41" },
            Arch         : { it == "amd64" },
            BuildTime    : { it == "2021-01-28T21:33:12.000000000+00:00" },
            GitCommit    : { it == "46229ca1d815cfd4b50eb377ac75ad8300e13a85" },
            GoVersion    : { it == "go1.13.15" },
            KernelVersion: { it =~ "\\d.\\d{1,2}.\\d{1,2}\\w*" },
            MinAPIVersion: { it == "1.12" },
            Os           : { it == "linux" },
            Version      : { it =~ "\\d{1,2}.\\d{1,2}.\\d{1,2}\\w*" }]
      }
    }
    else if (LocalDocker.isNativeWindows()) {
      versionDetails = [
          ApiVersion   : { it == "1.40" },
          Arch         : { it == "amd64" },
          BuildTime    : { it == "2020-09-16T17:14:20.000000000+00:00" },
          GitCommit    : { it == "4484c46d9d" },
          GoVersion    : { it == "go1.13.15" },
          KernelVersion: { it =~ "\\d.\\d{1,2}.\\d{1,2}\\w*" },
          MinAPIVersion: { it == "1.24" },
          Os           : { it == "windows" },
          Version      : { it =~ "\\d{1,2}.\\d{1,2}.\\d{1,2}\\w*" }]
    }
    else {
      versionDetails = [
          ApiVersion   : { it == "1.41" },
          Arch         : { it == "amd64" },
          BuildTime    : { it == "2020-12-28T16:15:28.000000000+00:00" },
          GitCommit    : { it == "8891c58" },
          GoVersion    : { it == "go1.13.15" },
          KernelVersion: { it =~ "\\d.\\d{1,2}.\\d{1,2}\\w*" },
          MinAPIVersion: { it == "1.12" },
          Os           : { it == "linux" },
          Version      : { it =~ "\\d{1,2}.\\d{1,2}.\\d{1,2}\\w*" }]
    }
  }
}
