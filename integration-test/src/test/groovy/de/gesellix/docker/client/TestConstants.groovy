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
    // docker inspect --format "{{ json .Created }}, Id: {{ json .Id }}, Digests: {{ json .RepoDigests }}" gesellix/echo-server:2025-07-27T22-12-00
    if (LocalDocker.isNativeWindows()) {
      imageDigest = "gesellix/echo-server@sha256:e07c7a757334d40c56035c74d4e46a6c4b0295fe6cb8fd22c0db81b5f8670365"
      imageId = "sha256:8d5dfeabe0e079a04ceac6be266dbc13230ec1e018ac04ff1f244f62b465e49e"
      imageCreated = 1753647324
      volumeTarget = "C:/my-volume"
    } else {
      imageDigest = "gesellix/echo-server@sha256:e07c7a757334d40c56035c74d4e46a6c4b0295fe6cb8fd22c0db81b5f8670365"
      // this one works on GitHub
      imageId = "sha256:c59e7878cd83c4bc1703f8be1e440c00eed24ccb0e196dae234abea2fb39277f"
      imageCreated = 1753647218
      // this one works for containerd
//      imageId = "sha256:e07c7a757334d40c56035c74d4e46a6c4b0295fe6cb8fd22c0db81b5f8670365"
//      imageCreated = 1753647508
      volumeTarget = "/my-volume"
    }
    imageRepo = "gesellix/echo-server"
    imageTag = "2025-07-27T22-12-00"
    imageName = "$imageRepo:$imageTag"

    versionDetails = [
        ApiVersion   : { it in ["1.43", "1.44", "1.45", "1.46", "1.47", "1.48", "1.49", "1.50", "1.51"] },
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
