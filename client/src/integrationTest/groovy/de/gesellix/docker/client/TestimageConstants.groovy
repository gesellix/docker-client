package de.gesellix.docker.client

class TestimageConstants {

    final String imageRepo
    final String imageTag
    final String imageName
    final String imageDigest

    static TestimageConstants CONSTANTS = new TestimageConstants()

    TestimageConstants() {
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
            imageDigest = "sha256:f140404df2dffc6bb5ca2eb76fed759e4c26b743452550adcf6a3877381bef68"
        }
    }
}
