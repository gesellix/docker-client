package de.gesellix.docker.client

class TestConstants {

    final String imageRepo
    final String imageTag
    final String imageName
    final String imageDigest

    final Map<String, Closure<Boolean>> versionDetails = [:]

    static TestConstants CONSTANTS = new TestConstants()

    TestConstants() {
        if (LocalDocker.isNativeWindows()) {
            imageRepo = "gesellix/testimage"
            imageTag = "os-windows"
            imageDigest = "sha256:fd9e2bfa5acf34d40971f7749fcb560f3ef4423a814218055e5d124579ce7bd0"
            //imageDigest = "sha256:ad668e7a31ddd5df9fa481b983df0ea300045da865179cfe058503c6ef16237d"
        } else {
            imageRepo = "gesellix/testimage"
            imageTag = "os-linux"
            imageDigest = "sha256:0ce18ad10d281bef97fe2333a9bdcc2dbf84b5302f66d796fed73aac675320db"
        }
        imageName = "$imageRepo:$imageTag"

        // TODO consider checking the Docker api version instead of "TRAVIS"
        if (System.env.TRAVIS) {
            versionDetails = [
                    ApiVersion   : { it == "1.24" },
                    Arch         : { it == "amd64" },
                    BuildTime    : { it == "2016-10-26T21:44:32.558660410+00:00" },
                    GitCommit    : { it == "6b644ec" },
                    GoVersion    : { it == "go1.6.3" },
                    KernelVersion: { it =~ "\\d.\\d{1,2}.\\d{1,2}(-\\w+)?" },
                    MinAPIVersion: { it == null },
                    Os           : { it == "linux" },
                    Version      : { it == "1.12.3" }]
        } else if (LocalDocker.isNativeWindows()) {
            versionDetails = [
                    ApiVersion   : { it == "1.25" },
                    Arch         : { it == "amd64" },
                    BuildTime    : { it == "2017-01-18T16:20:26.924957224+00:00" },
                    GitCommit    : { it == "49bf474" },
                    GoVersion    : { it == "go1.7.3" },
                    KernelVersion: { it =~ "\\d.\\d{1,2}.\\d{1,2}(-\\w+)?" },
                    MinAPIVersion: { it == "1.24" },
                    Os           : { it == "windows" },
                    Version      : { it == "1.13.0" }]
        } else {
            versionDetails = [
                    ApiVersion   : { it == "1.26" },
                    Arch         : { it == "amd64" },
                    BuildTime    : { it == "2017-02-08T08:47:51.966588829+00:00" },
                    GitCommit    : { it == "092cba3" },
                    GoVersion    : { it == "go1.7.5" },
                    KernelVersion: { it =~ "\\d.\\d{1,2}.\\d{1,2}(-\\w+)?" },
                    MinAPIVersion: { it == "1.12" },
                    Os           : { it == "linux" },
                    Version      : { it == "1.13.1" }]
        }
    }
}
