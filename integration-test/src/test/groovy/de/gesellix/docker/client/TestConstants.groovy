package de.gesellix.docker.client

class TestConstants {

  final String imageRepo
  final String imageTag
  final String imageName
  final String imageDigest
  final int imageCreated
  final String volumeTarget

  final Map<String, Closure<Boolean>> versionDetails = [:]

  static TestConstants CONSTANTS = new TestConstants()

  TestConstants() {
    if (LocalDocker.isNativeWindows()) {
      imageRepo = "gesellix/echo-server"
      imageTag = "os-windows"
      imageDigest = "sha256:7a9afe60edd3affcafb76769e73f1bfc5c020cf6d30ba65a9cc5823b3dd40981"
      imageCreated = 1646088128
      volumeTarget = "C:/my-volume"
    }
    else {
      imageRepo = "gesellix/echo-server"
      imageTag = "os-linux"
      imageDigest = "sha256:e1071a8bb352a348c84c52dc129dc6fefce9c727e1446c1eb066e71d2a77ef08"
      imageCreated = 1646256712
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
