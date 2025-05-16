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
    // docker inspect --format "{{ json .Created }}, Id: {{ json .Id }}, Digests: {{ json .RepoDigests }}" gesellix/echo-server:2024-09-15T19-35-00
    if (LocalDocker.isNativeWindows()) {
      imageDigest = "gesellix/echo-server@sha256:de454e47bcd8ff5247545ce3c886c0af93be127b771fcd3bfc3651744252e335"
      imageId = "sha256:662e836a255b25d8c9060c325953027a5296ab8bf689794467361449ead0d1b8"
      imageCreated = 1726421760
      volumeTarget = "C:/my-volume"
    } else {
      imageDigest = "gesellix/echo-server@sha256:de454e47bcd8ff5247545ce3c886c0af93be127b771fcd3bfc3651744252e335"
      // this one works on GitHub
      imageId = "sha256:eba6f6644b5255fc5b6ab4dcd4b63495ae2d438723e7da4795ed52dc6d06856d"
      // this one works for containerd
//      imageId = "sha256:de454e47bcd8ff5247545ce3c886c0af93be127b771fcd3bfc3651744252e335"
      imageCreated = 1726421659
      volumeTarget = "/my-volume"
    }
    imageRepo = "gesellix/echo-server"
    imageTag = "2024-09-15T19-35-00"
    imageName = "$imageRepo:$imageTag"

    versionDetails = [
        ApiVersion   : { it in ["1.43", "1.44", "1.45", "1.46", "1.47", "1.48"] },
        Arch         : { it in ["amd64", "arm64"] },
        BuildTime    : { it =~ "\\d{4}-\\d{2}-\\d{2}T\\w+" },
        GitCommit    : { it =~ "\\w{6,}" },
        GoVersion    : { it =~ "go\\d+.\\d+.\\d+" },
        KernelVersion: { it =~ "\\d.\\d{1,2}.\\d{1,2}\\w*" },
        MinAPIVersion: { it == "1.12" },
        Os           : { it == "linux" },
        Version      : { it == "master" || it =~ "\\d{1,2}.\\d{1,2}.\\d{1,2}\\w*" }]
    if (LocalDocker.isNativeWindows()) {
      versionDetails.MinAPIVersion = { it == "1.24" }
      versionDetails.Os = { it == "windows" }
    }
  }
}
