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
            imageRepo = "gesellix/docker-client-testimage"
            imageTag = "os-windows"
        } else {
            imageRepo = "gesellix/testimage"
            imageTag = "os-linux"
        }
        imageName = "$imageRepo:$imageTag"

        if (LocalDocker.isNamedPipe()) {
            imageDigest = "sha256:c7e830fdce2919ea4c8f3b6461e621bfd02f5e0d50b214cfd5317f49103d0b30"
        } else {
            imageDigest = "sha256:0ce18ad10d281bef97fe2333a9bdcc2dbf84b5302f66d796fed73aac675320db"
        }

        if (System.env.TRAVIS == true) {
            versionDetails = [
                    ApiVersion   : { it == "1.24" },
                    Arch         : { it == "amd64" },
                    BuildTime    : { it == "2016-10-26T21:44:32.558660410+00:00" },
                    GitCommit    : { it == "6b644ec" },
                    GoVersion    : { it == "go1.6.3" },
                    KernelVersion: { it =~ "\\d.\\d{1,2}.\\d{1,2}(-\\w+)?" },
                    MinAPIVersion: { it == "1.12" },
                    Os           : { it == "linux" },
                    Version      : { it == "1.12.3" }]
        } else {
            versionDetails = [
                    ApiVersion   : { it == "1.25" },
                    Arch         : { it == "amd64" },
                    BuildTime    : { it == "2016-12-17T01:34:17.687787854+00:00" },
                    GitCommit    : { it == "88862e7" },
                    GoVersion    : { it == "go1.7.3" },
                    KernelVersion: { it =~ "\\d.\\d{1,2}.\\d{1,2}(-\\w+)?" },
                    MinAPIVersion: { it == "1.12" },
                    Os           : { it == "linux" },
                    Version      : { it == "1.13.0-rc4" }]
        }
    }
}
