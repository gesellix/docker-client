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
            imageDigest = "sha256:5880c6a3a386d67cd02b0ee4684709f9c966225270e97e0396157894ae74dbe6"
        } else {
            imageRepo = "gesellix/testimage"
            imageTag = "os-linux"
            imageDigest = "sha256:0ce18ad10d281bef97fe2333a9bdcc2dbf84b5302f66d796fed73aac675320db"
        }
        imageName = "$imageRepo:$imageTag"

        // TODO consider checking the Docker api version instead of "TRAVIS"
        if (System.env.TRAVIS) {
            versionDetails = [
                    ApiVersion   : { it == "1.32" },
                    Arch         : { it == "amd64" },
                    BuildTime    : { it == "2017-09-26T22:41:20.000000000+00:00" },
                    GitCommit    : { it == "afdb6d4" },
                    GoVersion    : { it == "go1.8.3" },
                    KernelVersion: { it =~ "\\d.\\d{1,2}.\\d{1,2}(-\\w+)?" },
                    MinAPIVersion: { it == "1.12" },
                    Os           : { it == "linux" },
                    Version      : { it == "17.09.0-ce" }]
        } else if (LocalDocker.isNativeWindows()) {
            versionDetails = [
                    ApiVersion   : { it == "1.39" },
                    Arch         : { it == "amd64" },
                    BuildTime    : { it == "2018-09-06T22:57:45.000000000+00:00" },
                    GitCommit    : { it == "78a6bdb" },
                    GoVersion    : { it == "go1.10.3" },
                    KernelVersion: { it =~ "\\d.\\d{1,2}.\\d{1,2}(-\\w+)?" },
                    MinAPIVersion: { it == "1.24" },
                    Os           : { it == "windows" },
                    Version      : { it == "18.09.0-ce-beta1" }]
        } else {
            versionDetails = [
                    ApiVersion   : { it == "1.39" },
                    Arch         : { it == "amd64" },
                    BuildTime    : { it == "2018-09-06T22:49:35.000000000+00:00" },
                    GitCommit    : { it == "78a6bdb" },
                    GoVersion    : { it == "go1.10.3" },
                    KernelVersion: { it =~ "\\d.\\d{1,2}.\\d{1,2}(-\\w+)?" },
                    MinAPIVersion: { it == "1.12" },
                    Os           : { it == "linux" },
                    Version      : { it == "18.09.0-ce-beta1" }]
        }
    }
}
