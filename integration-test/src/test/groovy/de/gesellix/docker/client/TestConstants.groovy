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
            imageDigest = "sha256:5880c6a3a386d67cd02b0ee4684709f9c966225270e97e0396157894ae74dbe6"
            imageCreated = 1575214105
        }
        else {
            imageRepo = "gesellix/testimage"
            imageTag = "os-linux"
            imageDigest = "sha256:0ce18ad10d281bef97fe2333a9bdcc2dbf84b5302f66d796fed73aac675320db"
            imageCreated = 1483093519
        }
        imageName = "$imageRepo:$imageTag"

        if (System.getenv("TRAVIS")) {
            // TODO consider checking the Docker api version instead of "TRAVIS"
            versionDetails = [
                    ApiVersion   : { it == "1.32" },
                    Arch         : { it == "amd64" },
                    BuildTime    : { it == "2017-09-26T22:41:20.000000000+00:00" },
                    GitCommit    : { it == "afdb6d4" },
                    GoVersion    : { it == "go1.8.3" },
                    KernelVersion: { it =~ "\\d.\\d{1,2}.\\d{1,2}\\w*" },
                    MinAPIVersion: { it == "1.12" },
                    Os           : { it == "linux" },
                    Version      : { it =~ "\\d{1,2}.\\d{1,2}.\\d{1,2}\\w*" }]
        }
        else if (System.getenv("GITHUB_ACTOR")) {
            // TODO consider checking the Docker api version instead of "GITHUB_ACTOR"
            if (LocalDocker.isNativeWindows()) {
                versionDetails = [
                        ApiVersion   : { it == "1.40" },
                        Arch         : { it == "amd64" },
                        BuildTime    : { it == "08/05/2020 19:26:41" },
                        GitCommit    : { it == "f295753ffd" },
                        GoVersion    : { it == "go1.13.13" },
                        KernelVersion: { it =~ "\\d.\\d{1,2}.\\d{1,2}\\w*" },
                        MinAPIVersion: { it == "1.24" },
                        Os           : { it == "windows" },
                        Version      : { it =~ "\\d{1,2}.\\d{1,2}.\\d{1,2}\\w*" }]
            }
            else {
                versionDetails = [
                        ApiVersion   : { it == "1.40" },
                        Arch         : { it == "amd64" },
                        BuildTime    : { it == "2018-03-12T00:00:00.000000000+00:00" },
                        GitCommit    : { it == "9dc6525e6118a25fab2be322d1914740ea842495" },
                        GoVersion    : { it == "go1.13.11" },
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
                    ApiVersion   : { it == "1.40" },
                    Arch         : { it == "amd64" },
                    BuildTime    : { it == "2020-09-16T17:07:04.000000000+00:00" },
                    GitCommit    : { it == "4484c46d9d" },
                    GoVersion    : { it == "go1.13.15" },
                    KernelVersion: { it =~ "\\d.\\d{1,2}.\\d{1,2}\\w*" },
                    MinAPIVersion: { it == "1.12" },
                    Os           : { it == "linux" },
                    Version      : { it =~ "\\d{1,2}.\\d{1,2}.\\d{1,2}\\w*" }]
        }
    }
}
