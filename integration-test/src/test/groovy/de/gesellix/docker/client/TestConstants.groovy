package de.gesellix.docker.client

class TestConstants {

  final String imageRepo
  final String imageTag
  final String imageName
  final String imageDigest
  final int imageCreated

  final Map<String, Closure<Boolean>> versionDetails = [:]

  static TestConstants CONSTANTS = new TestConstants()

  TestConstants() {
    if (LocalDocker.isNativeWindows()) {
      imageRepo = "gesellix/testimage"
      imageTag = "os-windows"
      imageDigest = "sha256:21ef7773618aed0ad91c8171698f8ad75ae5927fb8e68b444e9c716cb6450447"
      imageCreated = 1625426527
    }
    else {
      imageRepo = "gesellix/testimage"
      imageTag = "os-linux"
      imageDigest = "sha256:da16ff11360dedb8bbb3f2a89da87527fb5e7d956e9efcb032df123caf2d5c9f"
      imageCreated = 1625401515
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
