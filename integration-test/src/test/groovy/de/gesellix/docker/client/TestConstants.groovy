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
    // docker inspect --format "{{ json .Created }}, Id: {{ json .Id }}, Digests: {{ json .RepoDigests }}" gesellix/echo-server:2024-07-28T18-30-00
    if (LocalDocker.isNativeWindows()) {
      imageDigest = "gesellix/echo-server@sha256:f7700fa4af53a3c4405dc728388a3b6d4b9d1271f08dedc87bee12748d18e7b2"
      imageId = "sha256:692a91727fb2b3b560061203e0844dc44c07ac402856110f78b70bc00fedb6da"
      imageCreated = 1722184400
      volumeTarget = "C:/my-volume"
    } else {
      imageDigest = "gesellix/echo-server@sha256:f7700fa4af53a3c4405dc728388a3b6d4b9d1271f08dedc87bee12748d18e7b2"
      // this one works on GitHub
      imageId = "sha256:53f46ff30384f9b3b2700500cbaaa4041b5748e060f77bbaba6e26affbeb6d83"
      // this one works for containerd
//      imageId = "sha256:838c203a87cc711eb182d6f8fed10fe6894cdc70fe3dedd0dc1e6111987836b5"
      imageCreated = 1722184237
      volumeTarget = "/my-volume"
    }
    imageRepo = "gesellix/echo-server"
    imageTag = "2024-07-28T18-30-00"
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
