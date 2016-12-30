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
            imageDigest = "sha256:c49c8696daa341d15e417928b1036c2a7459d7d9e9010333cc8c560558f18891"
        }
    }
}
