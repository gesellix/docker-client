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
    // docker inspect --format "{{ json .Created }}, Id: {{ json .Id }}, Digests: {{ json .RepoDigests }}" gesellix/echo-server:2.0.0-202606131519
    if (LocalDocker.isNativeWindows()) {
      imageDigest = "gesellix/echo-server@sha256:2b721139e29a631c701f544855c0a466caad1e591433b5fa91f4988a8a52277a"
      imageId = "sha256:83645f4f5e4284555f624b0a7c61af2aee44e75e196072c5311a75d300058f73"
      imageCreated = 1781357606
      volumeTarget = "C:/my-volume"
    } else {
      imageDigest = "gesellix/echo-server@sha256:2b721139e29a631c701f544855c0a466caad1e591433b5fa91f4988a8a52277a"
      // this one works on GitHub
      imageId = "sha256:b26757d4392fdba15dbf20fe3c5e6c13b6fbcf9324532f8ae3bfea104511e4bc"
      imageCreated = 1781356898
      // this one works for containerd
//      imageId = "sha256:2b721139e29a631c701f544855c0a466caad1e591433b5fa91f4988a8a52277a"
//      imageCreated = 1781357278
      volumeTarget = "/my-volume"
    }
    imageRepo = "gesellix/echo-server"
    imageTag = "2.0.0-202606131519"
    imageName = "$imageRepo:$imageTag"

    versionDetails = [
        ApiVersion   : { it in ["1.43", "1.44", "1.45", "1.46", "1.47", "1.48", "1.49", "1.50", "1.51", "1.52", "1.53"] },
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
