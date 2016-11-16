package de.gesellix.docker.client

class TestimageConstants {

    final String imageRepo
    final String imageTag
    final String imageName
    final String imageDigest

    static TestimageConstants CONSTANTS = new TestimageConstants()

    TestimageConstants() {
        imageRepo = "gesellix/docker-client-testimage"
        imageTag = LocalDocker.isNativeWindows() ? "os-windows" : "os-linux"
        imageName = "$imageRepo:$imageTag"
        imageDigest = LocalDocker.isNamedPipe() ? "sha256:c7e830fdce2919ea4c8f3b6461e621bfd02f5e0d50b214cfd5317f49103d0b30" : "sha256:a766bfd819b1116a6172c2a7a11a9e90f25222c00bdd740a5ad2575b000a7339"
    }
}
