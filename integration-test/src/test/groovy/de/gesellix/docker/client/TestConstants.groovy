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
      imageDigest = "gesellix/echo-server@sha256:53d94aa81d0bb50e79a080b50d18efb4bc042a7eb7ad12f6c1b9091dda11b731"
      imageId = "sha256:f74494ddddd6f19207a4606e243480faafa52d974207c0bb74b8c71195354cbc"
      imageCreated = 1649450241
      volumeTarget = "C:/my-volume"
    }
    else {
      imageDigest = "gesellix/echo-server@sha256:53d94aa81d0bb50e79a080b50d18efb4bc042a7eb7ad12f6c1b9091dda11b731"
      imageId = "sha256:a07dead7f020c6f0602181f2ca3920634df1cf11cf8de5ca443e25ea4d334a61"
      imageCreated = 1649449755
      volumeTarget = "/my-volume"
    }
    imageRepo = "gesellix/echo-server"
    imageTag = "2022-04-08T22-27-00"
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
