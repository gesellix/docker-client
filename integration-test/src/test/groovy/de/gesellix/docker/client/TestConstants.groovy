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
    // docker inspect --format "{{ json .Created }}, Id: {{ json .Id }}, Digests: {{ json .RepoDigests }}" gesellix/echo-server:2023-12-19T09-50-00
    if (LocalDocker.isNativeWindows()) {
      imageDigest = "gesellix/echo-server@sha256:838c203a87cc711eb182d6f8fed10fe6894cdc70fe3dedd0dc1e6111987836b5"
      imageId = "sha256:3b98f60f6aee437e6a82470c4bdb0216ce512d162e218081cb7cd5d3977ba582"
      imageCreated = 1702976162
      volumeTarget = "C:/my-volume"
    } else {
      imageDigest = "gesellix/echo-server@sha256:838c203a87cc711eb182d6f8fed10fe6894cdc70fe3dedd0dc1e6111987836b5"
      imageId = "sha256:838c203a87cc711eb182d6f8fed10fe6894cdc70fe3dedd0dc1e6111987836b5"
      imageCreated = 1702975999
      volumeTarget = "/my-volume"
    }
    imageRepo = "gesellix/echo-server"
    imageTag = "2023-12-19T09-50-00"
    imageName = "$imageRepo:$imageTag"

    versionDetails = [
        ApiVersion   : { it in ["1.43", "1.44"] },
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
